package com.unilus.careconnect;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/counsellor/profile")
public class CounsellorProfileController {

    private final JdbcTemplate jdbc;

    public CounsellorProfileController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private UUID profileId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("You must be logged in.");
        }
        return jdbc.query("""
                SELECT cp.id
                FROM credentials.counsellor_profiles cp
                JOIN credentials.users u ON u.id = cp.user_id
                WHERE lower(u.email) = lower(?) AND u.enabled = TRUE
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null,
                authentication.getName());
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            UUID id = profileId(authentication);
            if (id == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Counsellor profile not found."));
            return ResponseEntity.ok(jdbc.queryForMap("""
                    SELECT display_name AS "displayName",
                           professional_title AS "professionalTitle",
                           biography
                    FROM credentials.counsellor_profiles WHERE id = ?
                    """, id));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody ProfileUpdate request,
            Authentication authentication) {
        try {
            UUID id = profileId(authentication);
            if (id == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Counsellor profile not found."));
            if (request == null || request.displayName() == null
                    || request.displayName().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Your name is required."));
            }
            String name = request.displayName().trim();
            String title = normalize(request.professionalTitle());
            String biography = normalize(request.biography());
            if (name.length() > 150 || (title != null && title.length() > 150)
                    || (biography != null && biography.length() > 5000)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Name/title must be at most 150 characters and biography at most 5000 characters."));
            }
            jdbc.update("""
                    UPDATE credentials.counsellor_profiles
                    SET display_name = ?, professional_title = ?, biography = ?
                    WHERE id = ?
                    """, name, title, biography, id);
            return ResponseEntity.ok(Map.of(
                    "displayName", name,
                    "professionalTitle", title == null ? "" : title,
                    "biography", biography == null ? "" : biography,
                    "message", "Your profile has been saved."));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ProfileUpdate(
            String displayName,
            String professionalTitle,
            String biography) {}
}
