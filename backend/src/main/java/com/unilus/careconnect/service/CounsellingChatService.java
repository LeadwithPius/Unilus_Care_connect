package com.unilus.careconnect.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CounsellingChatService {

    private final JdbcTemplate jdbc;

    public CounsellingChatService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private record Identity(UUID userId, UUID profileId) {}

    private Identity identity(String email, String role) {
        try {
            if ("STUDENT".equals(role)) {
                return jdbc.queryForObject("""
                    SELECT u.id AS user_id, sil.student_profile_id AS profile_id
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                    WHERE lower(u.email) = lower(?) AND u.enabled = TRUE
                    """, (rs, row) -> new Identity(
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("profile_id", UUID.class)
                    ), email);
            }
            if ("COUNSELLOR".equals(role)) {
                return jdbc.queryForObject("""
                    SELECT u.id AS user_id, cp.id AS profile_id
                    FROM credentials.users u
                    JOIN credentials.counsellor_profiles cp ON cp.user_id = u.id
                    WHERE lower(u.email) = lower(?) AND u.enabled = TRUE
                    """, (rs, row) -> new Identity(
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("profile_id", UUID.class)
                    ), email);
            }
            throw new IllegalArgumentException("This account cannot access counselling chat.");
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Your enabled CareConnect profile could not be found.");
        }
    }

    @Transactional
    public List<Map<String, Object>> conversations(String email, String role) {
        Identity identity = identity(email, role);

        if ("STUDENT".equals(role)) {
            return jdbc.queryForList("""
                SELECT cs.id AS "conversationId",
                       cp.display_name AS "participantName",
                       cp.professional_title AS "participantTitle",
                       a.scheduled_start AS "scheduledStart",
                       a.status AS "appointmentStatus",
                       cs.session_status AS "sessionStatus",
                       latest.body AS "lastMessage",
                       latest.created_at AS "lastMessageAt",
                       (cs.session_status IN ('SCHEDULED', 'IN_PROGRESS')
                         AND a.status IN ('CONFIRMED', 'CHECKED_IN')) AS "canSend"
                FROM wellness.counselling_sessions cs
                JOIN wellness.appointments a ON a.id = cs.appointment_id
                JOIN credentials.counsellor_profiles cp
                  ON cp.id = cs.counsellor_profile_id
                LEFT JOIN LATERAL (
                    SELECT m.body, m.created_at
                    FROM wellness.counselling_messages m
                    WHERE m.counselling_session_id = cs.id
                    ORDER BY m.created_at DESC, m.id DESC
                    LIMIT 1
                ) latest ON TRUE
                WHERE cs.student_profile_id = ?
                ORDER BY COALESCE(latest.created_at, a.scheduled_start) DESC,
                         cs.id
                """, identity.profileId());
        }

        jdbc.update("""
            INSERT INTO wellness.counsellor_student_aliases
                (counsellor_profile_id, student_profile_id, alias_code)
            SELECT DISTINCT cs.counsellor_profile_id,
                   cs.student_profile_id,
                   upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 10))
            FROM wellness.counselling_sessions cs
            WHERE cs.counsellor_profile_id = ?
            ON CONFLICT DO NOTHING
            """, identity.profileId());

        return jdbc.queryForList("""
            SELECT cs.id AS "conversationId",
                   'Student #' || alias.alias_code AS "participantName",
                   NULL::VARCHAR AS "participantTitle",
                   a.scheduled_start AS "scheduledStart",
                   a.status AS "appointmentStatus",
                   cs.session_status AS "sessionStatus",
                   latest.body AS "lastMessage",
                   latest.created_at AS "lastMessageAt",
                   (cs.session_status IN ('SCHEDULED', 'IN_PROGRESS')
                     AND a.status IN ('CONFIRMED', 'CHECKED_IN')) AS "canSend"
            FROM wellness.counselling_sessions cs
            JOIN wellness.appointments a ON a.id = cs.appointment_id
            JOIN wellness.counsellor_student_aliases alias
              ON alias.counsellor_profile_id = cs.counsellor_profile_id
             AND alias.student_profile_id = cs.student_profile_id
            LEFT JOIN LATERAL (
                SELECT m.body, m.created_at
                FROM wellness.counselling_messages m
                WHERE m.counselling_session_id = cs.id
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT 1
            ) latest ON TRUE
            WHERE cs.counsellor_profile_id = ?
            ORDER BY COALESCE(latest.created_at, a.scheduled_start) DESC,
                     cs.id
            """, identity.profileId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> messages(
            String email, String role, UUID conversationId) {
        Identity identity = identity(email, role);
        requireConversationAccess(identity.profileId(), role, conversationId);

        return jdbc.queryForList("""
            SELECT recent.id AS "messageId",
                   recent.sender_role AS "senderRole",
                   recent.body AS "content",
                   recent.created_at AS "createdAt",
                   recent.mine AS "mine"
            FROM (
                SELECT m.id, m.sender_role, m.body, m.created_at,
                       (m.sender_user_id = ?) AS mine
                FROM wellness.counselling_messages m
                WHERE m.counselling_session_id = ?
                ORDER BY m.created_at DESC, m.id DESC
                LIMIT 200
            ) recent
            ORDER BY recent.created_at ASC, recent.id ASC
            """, identity.userId(), conversationId);
    }

    @Transactional
    public Map<String, Object> sendMessage(
            String email, String role, UUID conversationId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Write a message before sending.");
        }
        String body = content.trim();
        if (body.length() > 4000) {
            throw new IllegalArgumentException("Messages must be 4,000 characters or fewer.");
        }

        Identity identity = identity(email, role);
        Map<String, Object> session;
        try {
            String ownerColumn = "STUDENT".equals(role)
                    ? "cs.student_profile_id" : "cs.counsellor_profile_id";
            session = jdbc.queryForMap("""
                SELECT cs.session_status AS "sessionStatus",
                       a.status AS "appointmentStatus"
                FROM wellness.counselling_sessions cs
                JOIN wellness.appointments a ON a.id = cs.appointment_id
                WHERE cs.id = ?
                  AND """ + ownerColumn + """ = ?
                FOR UPDATE OF cs
                """, conversationId, identity.profileId());
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("Conversation not found or you do not have access to it.");
        }

        String sessionStatus = (String) session.get("sessionStatus");
        String appointmentStatus = (String) session.get("appointmentStatus");
        if (!List.of("SCHEDULED", "IN_PROGRESS").contains(sessionStatus)
                || !List.of("CONFIRMED", "CHECKED_IN").contains(appointmentStatus)) {
            throw new IllegalStateException("This counselling conversation is read-only because the appointment is no longer active.");
        }

        UUID messageId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO wellness.counselling_messages
                (id, counselling_session_id, sender_user_id, sender_role, body)
            VALUES (?, ?, ?, ?, ?)
            """, messageId, conversationId, identity.userId(), role, body);

        return jdbc.queryForMap("""
            SELECT id AS "messageId",
                   sender_role AS "senderRole",
                   body AS "content",
                   created_at AS "createdAt",
                   TRUE AS "mine"
            FROM wellness.counselling_messages
            WHERE id = ?
            """, messageId);
    }

    private void requireConversationAccess(
            UUID profileId, String role, UUID conversationId) {
        String ownerColumn = "STUDENT".equals(role)
                ? "student_profile_id" : "counsellor_profile_id";
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wellness.counselling_sessions WHERE id = ? AND "
                        + ownerColumn + " = ?",
                Integer.class, conversationId, profileId);
        if (count == null || count != 1) {
            throw new IllegalArgumentException(
                    "Conversation not found or you do not have access to it.");
        }
    }
}
