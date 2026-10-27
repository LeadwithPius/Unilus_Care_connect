package com.unilus.careconnect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TriageWellbeingService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public TriageWellbeingService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> submitTriage(String email, TriageRequest request) {
        if (request == null || request.categoryCode() == null || request.categoryCode().isBlank()) {
            throw new IllegalArgumentException("Support category is required.");
        }
        if (request.impactLevel() == null || request.impactLevel() < 1 || request.impactLevel() > 5) {
            throw new IllegalArgumentException("Impact level must be between 1 and 5.");
        }
        if (request.immediateDanger() == null) {
            throw new IllegalArgumentException("Please answer the immediate-safety question.");
        }

        UUID studentId = studentId(email);
        UUID categoryId;
        try {
            categoryId = jdbc.queryForObject("""
                SELECT id FROM wellness.support_categories
                WHERE code = ? AND active = TRUE
                """, UUID.class, request.categoryCode().trim().toUpperCase(Locale.ROOT));
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("Invalid or inactive support category.");
        }

        String urgency = request.immediateDanger() ? "EMERGENCY"
                : request.impactLevel() >= 4 ? "URGENT"
                : request.impactLevel() == 3 ? "PRIORITY" : "ROUTINE";
        boolean escalation = request.immediateDanger() || request.impactLevel() >= 4;
        String action = request.immediateDanger() ? "IMMEDIATE_HUMAN_SUPPORT"
                : request.impactLevel() >= 4 ? "PROMPT_COUNSELLOR_SUPPORT"
                : request.impactLevel() == 3 ? "BOOK_COUNSELLOR_OR_REVIEW_SELF_HELP"
                : "SELF_HELP_OR_COUNSELLOR_BOOKING";

        Map<String, Object> answers = new LinkedHashMap<>();
        if (request.responses() != null) answers.putAll(request.responses());
        answers.put("impactLevel", request.impactLevel());
        answers.put("immediateDanger", request.immediateDanger());
        String json = toJson(answers);
        UUID id = UUID.randomUUID();

        jdbc.update("""
            INSERT INTO wellness.triage_assessments
                (id, student_profile_id, support_category_id, urgency_level,
                 recommended_action, escalation_required, started_at, completed_at,
                 questionnaire_version, responses)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'triage-v1', CAST(? AS jsonb))
            """, id, studentId, categoryId, urgency, action, escalation, json);

        audit(email, "TRIAGE_ASSESSMENT_CREATED", "triage_assessment", id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("urgencyLevel", urgency);
        result.put("recommendedAction", action);
        result.put("escalationRequired", escalation);
        result.put("message", request.immediateDanger()
                ? "Your answers suggest you may need immediate human support. Please contact a trusted person or local emergency service now, and do not wait for an app response."
                : escalation
                ? "Your responses suggest you should seek prompt support from a counsellor or trusted person."
                : "This check-in is not a diagnosis. You can explore self-help resources or book a counsellor if you would like support.");
        result.put("createdAt", jdbc.queryForObject(
                "SELECT created_at FROM wellness.triage_assessments WHERE id = ?",
                java.time.OffsetDateTime.class, id));
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myTriageHistory(String email) {
        UUID studentId = studentId(email);
        return jdbc.queryForList("""
            SELECT id, urgency_level AS "urgencyLevel",
                   recommended_action AS "recommendedAction",
                   escalation_required AS "escalationRequired",
                   questionnaire_version AS "questionnaireVersion",
                   responses, created_at AS "createdAt"
            FROM wellness.triage_assessments
            WHERE student_profile_id = ?
            ORDER BY created_at DESC
            LIMIT 100
            """, studentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> activeQuestionnaire() {
        List<Map<String, Object>> versions = jdbc.queryForList("""
            SELECT version, title, description
            FROM wellness.wellbeing_questionnaire_versions
            WHERE active = TRUE
            ORDER BY created_at DESC
            LIMIT 1
            """);
        if (versions.isEmpty()) return List.of();
        String version = (String) versions.getFirst().get("version");
        List<Map<String, Object>> questions = jdbc.queryForList("""
            SELECT question_key AS "key", prompt, response_type AS "responseType",
                   display_order AS "displayOrder", required
            FROM wellness.wellbeing_questionnaire_questions
            WHERE questionnaire_version = ?
            ORDER BY display_order
            """, version);
        Map<String, Object> result = new LinkedHashMap<>(versions.getFirst());
        result.put("questions", questions);
        return List.of(result);
    }

    @Transactional
    public Map<String, Object> submitCheckIn(String email, CheckInRequest request) {
        if (request == null || request.responses() == null || request.responses().isEmpty()) {
            throw new IllegalArgumentException("Questionnaire responses are required.");
        }
        UUID studentId = studentId(email);
        String version = request.questionnaireVersion() == null
                ? "wellbeing-v1" : request.questionnaireVersion().trim();

        List<Map<String, Object>> questions = jdbc.queryForList("""
            SELECT question_key, response_type, required
            FROM wellness.wellbeing_questionnaire_questions q
            JOIN wellness.wellbeing_questionnaire_versions v
              ON v.version = q.questionnaire_version
            WHERE q.questionnaire_version = ? AND v.active = TRUE
            """, version);
        if (questions.isEmpty()) throw new IllegalArgumentException("Questionnaire version is not active.");

        Set<String> allowed = new HashSet<>();
        Set<String> required = new HashSet<>();
        Map<String, String> types = new HashMap<>();
        for (Map<String, Object> q : questions) {
            String key = (String) q.get("question_key");
            allowed.add(key);
            types.put(key, (String) q.get("response_type"));
            if (Boolean.TRUE.equals(q.get("required"))) required.add(key);
        }
        if (!allowed.containsAll(request.responses().keySet())) {
            throw new IllegalArgumentException("Responses contain unknown questionnaire keys.");
        }
        if (!request.responses().keySet().containsAll(required)) {
            throw new IllegalArgumentException("Please answer all required questions.");
        }
        for (var entry : request.responses().entrySet()) {
            if (!"SCALE_1_5".equals(types.get(entry.getKey()))) {
                throw new IllegalArgumentException("Unsupported question response type.");
            }
            Object value = entry.getValue();
            if (!(value instanceof Number n) || n.doubleValue() != n.intValue()
                    || n.intValue() < 1 || n.intValue() > 5) {
                throw new IllegalArgumentException("Each scale response must be a whole number from 1 to 5.");
            }
        }

        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO wellness.wellbeing_check_ins
                (id, student_profile_id, questionnaire_version, responses)
            VALUES (?, ?, ?, CAST(? AS jsonb))
            """, id, studentId, version, toJson(request.responses()));
        audit(email, "WELLBEING_CHECKIN_CREATED", "wellbeing_check_in", id);
        return Map.of("id", id, "questionnaireVersion", version,
                "responses", request.responses(),
                "message", "Check-in saved. These reflections are not a diagnosis.");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> myCheckIns(String email) {
        return checkInsForStudent(studentId(email));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> counsellorCheckIns(String email, UUID studentProfileId) {
        UUID counsellorId;
        try {
            counsellorId = jdbc.queryForObject("""
                SELECT cp.id FROM credentials.counsellor_profiles cp
                JOIN credentials.users u ON u.id = cp.user_id
                WHERE lower(u.email) = lower(?) AND u.enabled = TRUE
                """, UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("Counsellor profile not found.");
        }
        Integer authorized = jdbc.queryForObject("""
            SELECT COUNT(*) FROM wellness.counselling_sessions
            WHERE student_profile_id = ? AND counsellor_profile_id = ?
              AND session_status IN ('IN_PROGRESS', 'COMPLETED')
            """, Integer.class, studentProfileId, counsellorId);
        if (authorized == null || authorized == 0) {
            audit(email, "WELLBEING_ACCESS_DENIED", "student_profile", studentProfileId);
            throw new SecurityException("You are not authorized to view this student's wellbeing history.");
        }
        audit(email, "WELLBEING_HISTORY_VIEWED", "student_profile", studentProfileId);
        return checkInsForStudent(studentProfileId);
    }

    private List<Map<String, Object>> checkInsForStudent(UUID studentId) {
        return jdbc.queryForList("""
            SELECT id, questionnaire_version AS "questionnaireVersion",
                   responses, created_at AS "createdAt"
            FROM wellness.wellbeing_check_ins
            WHERE student_profile_id = ?
            ORDER BY created_at DESC
            LIMIT 100
            """, studentId);
    }

    private UUID studentId(String email) {
        try {
            return jdbc.queryForObject("""
                SELECT sil.student_profile_id
                FROM credentials.users u
                JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                WHERE lower(u.email) = lower(?)
                """, UUID.class, email);
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("No student profile is linked to this account.");
        }
    }

    private void audit(String email, String eventType, String entityType, UUID entityId) {
        jdbc.update("""
            INSERT INTO credentials.audit_events (user_id, event_type, outcome, entity_type, entity_id)
            SELECT id, ?, 'SUCCESS', ?, ? FROM credentials.users WHERE lower(email) = lower(?)
            """, eventType, entityType, entityId, email);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Responses could not be processed.");
        }
    }

    public record TriageRequest(String categoryCode, Integer impactLevel,
                                Boolean immediateDanger, Map<String, Object> responses) {}
    public record CheckInRequest(String questionnaireVersion, Map<String, Object> responses) {}
}
