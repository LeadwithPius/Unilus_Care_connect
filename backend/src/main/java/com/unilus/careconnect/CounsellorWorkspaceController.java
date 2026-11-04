package com.unilus.careconnect;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/counsellor/profile")
public class CounsellorWorkspaceController {

    private final JdbcTemplate jdbcTemplate;

    public CounsellorWorkspaceController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        try {
            return ResponseEntity.ok(jdbcTemplate.queryForMap("""
                    SELECT cp.id AS "profileId",
                           cp.display_name AS "displayName",
                           cp.professional_title AS "professionalTitle",
                           cp.biography,
                           cp.accepting_appointments AS "acceptingAppointments"
                    FROM credentials.counsellor_profiles cp
                    JOIN credentials.users u ON u.id = cp.user_id
                    WHERE lower(u.email) = lower(?)
                      AND u.enabled = TRUE
                    """, authentication.getName()));
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No enabled counsellor profile is linked to this account."));
        }
    }

    @PatchMapping("/accepting-appointments")
    public ResponseEntity<?> updateAcceptingAppointments(
            Authentication authentication,
            @RequestBody AcceptingAppointmentsRequest request) {
        if (request == null || request.acceptingAppointments() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "acceptingAppointments must be true or false."));
        }

        int updated = jdbcTemplate.update("""
                UPDATE credentials.counsellor_profiles cp
                SET accepting_appointments = ?
                FROM credentials.users u
                WHERE cp.user_id = u.id
                  AND lower(u.email) = lower(?)
                  AND u.enabled = TRUE
                """, request.acceptingAppointments(), authentication.getName());

        if (updated != 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No enabled counsellor profile is linked to this account."));
        }

        return ResponseEntity.ok(Map.of(
                "acceptingAppointments", request.acceptingAppointments(),
                "message", request.acceptingAppointments()
                        ? "You are now accepting appointment requests."
                        : "You are no longer accepting new appointment requests."
        ));
    }

    public record AcceptingAppointmentsRequest(Boolean acceptingAppointments) {}
}
