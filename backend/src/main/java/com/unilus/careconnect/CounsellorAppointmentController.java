package com.unilus.careconnect;

import com.unilus.careconnect.service.CounsellorAppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/counsellor/appointments")
public class CounsellorAppointmentController {
    private final CounsellorAppointmentService service;

    public CounsellorAppointmentController(CounsellorAppointmentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(service.listAppointments(authentication.getName(), status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{appointmentId}/decision")
    public ResponseEntity<?> decide(
            Authentication authentication,
            @PathVariable UUID appointmentId,
            @RequestBody DecisionRequest request) {
        try {
            if (request == null || request.decision() == null) {
                throw new IllegalArgumentException("Decision is required.");
            }
            return ResponseEntity.ok(service.decideRequest(
                    authentication.getName(), appointmentId, request.decision()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{appointmentId}/attendance")
    public ResponseEntity<?> attendance(
            Authentication authentication,
            @PathVariable UUID appointmentId,
            @RequestBody AttendanceRequest request) {
        try {
            if (request == null || request.action() == null) {
                throw new IllegalArgumentException("Attendance action is required.");
            }
            return ResponseEntity.ok(service.markAttendance(
                    authentication.getName(), appointmentId, request.action()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    public record DecisionRequest(String decision) {}
    public record AttendanceRequest(String action) {}
}
