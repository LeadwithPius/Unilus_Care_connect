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

    public record AdminCounsellorEntry(
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
