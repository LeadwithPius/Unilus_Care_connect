
package com.unilus.careconnect;

import com.unilus.careconnect.entity.CounsellorAvailability;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.repository.CounsellorProfileRepository;
import com.unilus.careconnect.repository.UserAccountRepository;
import com.unilus.careconnect.repository.UserRoleRepository;
import com.unilus.careconnect.service.CounsellorAvailabilityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/counsellor/availability")
public class CounsellorAvailabilityController {

    private final CounsellorAvailabilityService availabilityService;
    private final UserAccountRepository userAccountRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;
    private final UserRoleRepository userRoleRepository;

    public CounsellorAvailabilityController(
            CounsellorAvailabilityService availabilityService,
            UserAccountRepository userAccountRepository,
            CounsellorProfileRepository counsellorProfileRepository,
            UserRoleRepository userRoleRepository) {

        this.availabilityService = availabilityService;
        this.userAccountRepository = userAccountRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
        this.userRoleRepository = userRoleRepository;
    }

    private CounsellorProfile getLoggedInCounsellor(
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "You must be logged in to manage availability."
            );
        }

        UserAccount user = userAccountRepository
                .findByEmail(authentication.getName().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User account not found."
                ));

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalStateException(
                    "Your account must be active."
            );
        }

        if (!userRoleRepository.existsByUserIdAndRoleName(
                user.getId(), "COUNSELLOR")) {

            throw new IllegalStateException(
                    "Only approved counsellors can manage availability."
            );
        }

        CounsellorProfile profile = counsellorProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Counsellor profile not found."
                ));

        if (!"ACTIVE".equalsIgnoreCase(profile.getAccountStatus())) {
            throw new IllegalStateException(
                    "Your counsellor profile is not active."
            );
        }

        return profile;
    }

    @GetMapping
    public ResponseEntity<?> getAvailability(
            @RequestParam(defaultValue = "true") boolean availableOnly,
            Authentication authentication) {

        try {
            CounsellorProfile profile =
                    getLoggedInCounsellor(authentication);

            List<CounsellorAvailability> periods =
                    availabilityService.getAvailability(
                            profile.getId(),
                            availableOnly
                    );

            return ResponseEntity.ok(periods);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("error", exception.getMessage())
            );
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

            CounsellorProfile profile =
                    getLoggedInCounsellor(authentication);

            CounsellorAvailability availability =
                    availabilityService.addAvailability(
                            profile.getId(),
                            request.getDayOfWeek(),
                            request.getStartTime(),
                            request.getEndTime()
                    );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(availability);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", exception.getMessage())
            );
        }
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<?> deleteAvailability(
            @PathVariable Long availabilityId,
            Authentication authentication) {

        try {
            CounsellorProfile profile =
                    getLoggedInCounsellor(authentication);

            availabilityService.deleteAvailability(
                    profile.getId(),
                    availabilityId
            );

            return ResponseEntity.ok(
                    Map.of("message", "Availability deleted successfully.")
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", exception.getMessage())
            );
        }
    }

    public static class AvailabilityRequest {

        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;

        public AvailabilityRequest() {
        }

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }
}