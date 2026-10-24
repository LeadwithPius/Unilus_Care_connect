package com.unilus.careconnect;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.service.CounsellorAvailabilityService;
import com.unilus.careconnect.service.CounsellorAvailabilityService.AvailabilitySlot;

@RestController
@RequestMapping("/api/counsellor/availability")
public class CounsellorAvailabilityController {

    private final CounsellorAvailabilityService availabilityService;
    private final JdbcTemplate jdbcTemplate;

    public CounsellorAvailabilityController(
            CounsellorAvailabilityService availabilityService,
            JdbcTemplate jdbcTemplate) {

        this.availabilityService = availabilityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    private UUID getLoggedInCounsellorProfileId(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "You must be logged in to manage availability."
            );
        }

        boolean counsellorRole = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_COUNSELLOR".equals(
                                authority.getAuthority()
                        )
                );

        if (!counsellorRole) {
            throw new IllegalStateException(
                    "Only counsellors can manage availability."
            );
        }

        List<UUID> profiles = jdbcTemplate.query(
                """
                SELECT cp.id
                FROM credentials.counsellor_profiles cp
                JOIN credentials.users u
                    ON u.id = cp.user_id
                WHERE lower(u.email) = lower(?)
                  AND u.enabled = TRUE
                """,
                (rs, rowNum) ->
                        rs.getObject("id", UUID.class),
                authentication.getName()
        );

        if (profiles.isEmpty()) {
            throw new IllegalStateException(
                    "Counsellor profile not found."
            );
        }

        return profiles.getFirst();
    }

    @GetMapping
    public ResponseEntity<?> getAvailability(
            @RequestParam(defaultValue = "true")
            boolean openOnly,
            Authentication authentication) {

        try {
            UUID profileId =
                    getLoggedInCounsellorProfileId(authentication);

            List<AvailabilitySlot> periods =
                    availabilityService.getAvailability(
                            profileId,
                            openOnly
                    );

            return ResponseEntity.ok(periods);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            exception.getMessage()
                    ));

        } catch (IllegalStateException exception) {

            return ResponseEntity.status(
                    HttpStatus.FORBIDDEN
            ).body(Map.of(
                    "error",
                    exception.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> addAvailability(
            @RequestBody AvailabilityRequest request,
            Authentication authentication) {

        try {
            if (request == null) {
                throw new IllegalArgumentException(
                        "Availability details are required."
                );
            }

            UUID profileId =
                    getLoggedInCounsellorProfileId(authentication);

            AvailabilitySlot availability =
                    availabilityService.addAvailability(
                            profileId,
                            request.getStartsAt(),
                            request.getEndsAt()
                    );

            return ResponseEntity.status(
                    HttpStatus.CREATED
            ).body(availability);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            exception.getMessage()
                    ));

        } catch (IllegalStateException exception) {

            return ResponseEntity.status(
                    HttpStatus.CONFLICT
            ).body(Map.of(
                    "error",
                    exception.getMessage()
            ));
        }
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<?> deleteAvailability(
            @PathVariable UUID availabilityId,
            Authentication authentication) {

        try {
            UUID profileId =
                    getLoggedInCounsellorProfileId(authentication);

            availabilityService.deleteAvailability(
                    profileId,
                    availabilityId
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Availability deleted successfully."
                    )
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            exception.getMessage()
                    ));

        } catch (IllegalStateException exception) {

            return ResponseEntity.status(
                    HttpStatus.CONFLICT
            ).body(Map.of(
                    "error",
                    exception.getMessage()
            ));
        }
    }

    public static class AvailabilityRequest {

        private OffsetDateTime startsAt;
        private OffsetDateTime endsAt;

        public OffsetDateTime getStartsAt() {
            return startsAt;
        }

        public void setStartsAt(
                OffsetDateTime startsAt) {
            this.startsAt = startsAt;
        }

        public OffsetDateTime getEndsAt() {
            return endsAt;
        }

        public void setEndsAt(
                OffsetDateTime endsAt) {
            this.endsAt = endsAt;
        }
    }
}