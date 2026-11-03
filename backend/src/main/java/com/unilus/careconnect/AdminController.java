package com.unilus.careconnect;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.dto.CreateCounsellorRequest;
import com.unilus.careconnect.dto.PublishConsentDocumentRequest;
import com.unilus.careconnect.entity.ConsentDocument;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.service.ConsentService;
import com.unilus.careconnect.service.CounsellorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CounsellorService counsellorService;
    private final ConsentService consentService;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(
            CounsellorService counsellorService,
            ConsentService consentService,
            JdbcTemplate jdbcTemplate) {
        this.counsellorService = counsellorService;
        this.consentService = consentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        Map<String, Object> counts = jdbcTemplate.queryForMap("""
                SELECT
                    (SELECT COUNT(*) FROM credentials.student_identity_links) AS students,
                    (SELECT COUNT(*) FROM credentials.counsellor_profiles cp
                     JOIN credentials.users u ON u.id = cp.user_id
                     WHERE u.enabled = TRUE) AS activeCounsellors,
                    (SELECT COUNT(*) FROM wellness.appointments) AS appointments,
                    (SELECT COUNT(*) FROM wellness.education_resources
                     WHERE published = TRUE AND status = 'PUBLISHED') AS publishedResources,
                    (SELECT COUNT(*) FROM wellness.availability_slots
                     WHERE status = 'OPEN' AND starts_at > CURRENT_TIMESTAMP) AS openSlots
                """);
        List<Map<String, Object>> appointmentStatuses = jdbcTemplate.queryForList("""
                SELECT status, COUNT(*) AS total
                FROM wellness.appointments
                GROUP BY status
                ORDER BY status
                """);
        return ResponseEntity.ok(Map.of(
                "counts", counts,
                "appointmentStatuses", appointmentStatuses
        ));
    }

    @GetMapping("/students")
    public ResponseEntity<?> listStudents() {
        return ResponseEntity.ok(jdbcTemplate.queryForList("""
                SELECT
                    u.id AS "userId",
                    u.email,
                    sr.full_name AS "fullName",
                    sr.student_number AS "studentNumber",
                    u.enabled,
                    u.email_verified AS "emailVerified",
                    sr.created_at AS "registeredAt",
                    (SELECT COUNT(*) FROM wellness.appointments ap
                     JOIN wellness.student_profiles sp ON sp.id = ap.student_profile_id
                     JOIN credentials.student_identity_links sil2
                       ON sil2.student_profile_id = sp.id
                     WHERE sil2.user_id = u.id) AS "appointmentCount"
                FROM credentials.users u
                JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                JOIN credentials.student_registration_details sr ON sr.user_id = u.id
                JOIN credentials.user_roles ur ON ur.user_id = u.id
                JOIN credentials.roles r ON r.id = ur.role_id
                WHERE r.name = 'STUDENT'
                ORDER BY sr.created_at DESC, lower(sr.full_name)
                """));
    }

    @PatchMapping("/students/{userId}/status")
    public ResponseEntity<?> updateStudentStatus(
            @PathVariable UUID userId,
            @RequestBody AccountStatusRequest request) {
        if (request == null || request.enabled() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "enabled must be true or false."));
        }
        int updated = jdbcTemplate.update("""
                UPDATE credentials.users u
                SET enabled = ?
                WHERE u.id = ?
                  AND EXISTS (
                    SELECT 1 FROM credentials.user_roles ur
                    JOIN credentials.roles r ON r.id = ur.role_id
                    WHERE ur.user_id = u.id AND r.name = 'STUDENT'
                  )
                """, request.enabled(), userId);
        if (updated != 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Student account not found."));
        }
        return ResponseEntity.ok(Map.of("userId", userId, "enabled", request.enabled()));
    }

    @PatchMapping("/counsellors/{userId}/status")
    public ResponseEntity<?> updateCounsellorStatus(
            @PathVariable UUID userId,
            @RequestBody AccountStatusRequest request) {
        if (request == null || request.enabled() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "enabled must be true or false."));
        }
        int updated = jdbcTemplate.update("""
                UPDATE credentials.users u
                SET enabled = ?
                WHERE u.id = ?
                  AND EXISTS (
                    SELECT 1 FROM credentials.user_roles ur
                    JOIN credentials.roles r ON r.id = ur.role_id
                    WHERE ur.user_id = u.id AND r.name = 'COUNSELLOR'
                  )
                """, request.enabled(), userId);
        if (updated != 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Counsellor account not found."));
        }
        if (!request.enabled()) {
            jdbcTemplate.update("""
                    UPDATE credentials.counsellor_profiles
                    SET accepting_appointments = FALSE
                    WHERE user_id = ?
                    """, userId);
        }
        return ResponseEntity.ok(Map.of("userId", userId, "enabled", request.enabled()));
    }

    @GetMapping("/consent-documents")
    public ResponseEntity<?> listConsentDocuments() {
        return ResponseEntity.ok(consentService.listDocuments());
    }

    @GetMapping("/counsellors")
    public ResponseEntity<List<AdminCounsellorEntry>> listCounsellors() {
        List<AdminCounsellorEntry> counsellors = jdbcTemplate.query(
                """
                SELECT
                    cp.id AS profile_id,
                    u.id AS user_id,
                    u.email,
                    cp.display_name,
                    cp.professional_title,
                    cp.biography,
                    u.enabled,
                    u.email_verified,
                    cp.accepting_appointments,
                    cp.created_at,
                    (
                        SELECT COUNT(*)
                        FROM wellness.availability_slots av
                        WHERE av.counsellor_profile_id = cp.id
                          AND av.status = 'OPEN'
                          AND av.starts_at > CURRENT_TIMESTAMP
                    ) AS open_slots
                FROM credentials.counsellor_profiles cp
                JOIN credentials.users u ON u.id = cp.user_id
                JOIN credentials.user_roles ur ON ur.user_id = u.id
                JOIN credentials.roles r ON r.id = ur.role_id
                WHERE r.name = 'COUNSELLOR'
                ORDER BY cp.created_at DESC, cp.display_name
                """,
                (rs, rowNum) -> new AdminCounsellorEntry(
                        rs.getObject("profile_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("email"),
                        rs.getString("display_name"),
                        rs.getString("professional_title"),
                        rs.getString("biography"),
                        rs.getBoolean("enabled"),
                        rs.getBoolean("email_verified"),
                        rs.getBoolean("accepting_appointments"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getLong("open_slots")
                )
        );
        return ResponseEntity.ok(counsellors);
    }

    @PostMapping("/counsellors")
    public ResponseEntity<?> createCounsellor(
            @Valid @RequestBody CreateCounsellorRequest request) {
        try {
            CounsellorProfile profile = counsellorService.createCounsellor(
                    request.getEmail(),
                    request.getPassword(),
                    request.getDisplayName(),
                    request.getProfessionalTitle(),
                    request.getBiography());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseMapper.counsellor(profile));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/consent-documents")
    public ResponseEntity<?> publishConsentDocument(
            @RequestBody PublishConsentDocumentRequest request) {
        try {
            ConsentDocument document = consentService.publishDocument(
                    request.getPurposeCode(),
                    request.getVersionLabel(),
                    request.getTitle(),
                    request.getBody());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseMapper.consentDocument(document));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", exception.getMessage()));
        }
    }

    public record AccountStatusRequest(Boolean enabled) {}\n\n    public record AdminCounsellorEntry(
            UUID profileId,
            UUID userId,
            String email,
            String displayName,
            String professionalTitle,
            String biography,
            boolean enabled,
            boolean emailVerified,
            boolean acceptingAppointments,
            OffsetDateTime createdAt,
            long openSlots
    ) {}
}
