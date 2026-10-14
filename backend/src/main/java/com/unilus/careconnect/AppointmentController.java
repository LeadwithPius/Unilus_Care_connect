
package com.unilus.careconnect;

import com.unilus.careconnect.entity.Appointment;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.repository.CounsellorProfileRepository;
import com.unilus.careconnect.repository.UserAccountRepository;
import com.unilus.careconnect.repository.UserRoleRepository;
import com.unilus.careconnect.service.AppointmentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;

    public AppointmentController(
            AppointmentService appointmentService,
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            CounsellorProfileRepository counsellorProfileRepository) {

        this.appointmentService = appointmentService;
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
    }

    private UserAccount getActiveUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new IllegalStateException("Please log in first.");
        }

        UserAccount user = userAccountRepository
                .findByEmail(authentication.getName().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User account not found."
                ));

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalStateException("Your account is not active.");
        }

        return user;
    }

    private void requireRole(Long userId, String role) {
        if (!userRoleRepository.existsByUserIdAndRoleName(userId, role)) {
            throw new IllegalStateException(
                    "You are not authorized to perform this action."
            );
        }
    }

    private CounsellorProfile getActiveCounsellor(Long userId) {
        CounsellorProfile profile = counsellorProfileRepository
                .findByUserId(userId)
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

    @PostMapping
    public ResponseEntity<?> bookAppointment(
            @RequestBody BookingRequest request,
            Authentication authentication) {

        try {
            if (request == null) {
                throw new IllegalArgumentException(
                        "Booking details are required."
                );
            }

            UserAccount user = getActiveUser(authentication);
            requireRole(user.getId(), "STUDENT");

            Appointment appointment = appointmentService.bookAppointment(
                    user.getId(),
                    request.getCounsellorProfileId(),
                    request.getAppointmentDateTime(),
                    request.getDurationMinutes() == null
                            ? 60 : request.getDurationMinutes(),
                    request.getReason()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(appointment);

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

    @GetMapping("/student")
    public ResponseEntity<?> getStudentAppointments(
            Authentication authentication) {

        try {
            UserAccount user = getActiveUser(authentication);
            requireRole(user.getId(), "STUDENT");

            List<Appointment> appointments =
                    appointmentService.getStudentAppointments(user.getId());

            return ResponseEntity.ok(appointments);

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

    @GetMapping("/counsellor")
    public ResponseEntity<?> getCounsellorAppointments(
            Authentication authentication) {

        try {
            UserAccount user = getActiveUser(authentication);
            requireRole(user.getId(), "COUNSELLOR");

            CounsellorProfile profile = getActiveCounsellor(user.getId());

            List<Appointment> appointments =
                    appointmentService.getCounsellorAppointments(
                            profile.getId()
                    );

            return ResponseEntity.ok(appointments);

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

    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Long appointmentId,
            @RequestBody StatusRequest request,
            Authentication authentication) {

        try {
            UserAccount user = getActiveUser(authentication);
            requireRole(user.getId(), "COUNSELLOR");

            CounsellorProfile profile = getActiveCounsellor(user.getId());

            List<Appointment> appointments =
                    appointmentService.getCounsellorAppointments(
                            profile.getId()
                    );

            boolean ownsAppointment = appointments.stream()
                    .anyMatch(appointment ->
                            appointment.getId().equals(appointmentId));

            if (!ownsAppointment) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        Map.of("error",
                                "Appointment not found for this counsellor.")
                );
            }

            if (request == null || request.getStatus() == null) {
                throw new IllegalArgumentException(
                        "Appointment status is required."
                );
            }

            Appointment updated =
                    appointmentService.updateAppointmentStatus(
                            appointmentId,
                            request.getStatus()
                    );

            return ResponseEntity.ok(updated);

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

    public static class BookingRequest {

        private Long counsellorProfileId;
        private LocalDateTime appointmentDateTime;
        private Integer durationMinutes;
        private String reason;

        public BookingRequest() {
        }

        public Long getCounsellorProfileId() {
            return counsellorProfileId;
        }

        public void setCounsellorProfileId(Long counsellorProfileId) {
            this.counsellorProfileId = counsellorProfileId;
        }

        public LocalDateTime getAppointmentDateTime() {
            return appointmentDateTime;
        }

        public void setAppointmentDateTime(
                LocalDateTime appointmentDateTime) {
            this.appointmentDateTime = appointmentDateTime;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class StatusRequest {

        private String status;

        public StatusRequest() {
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}