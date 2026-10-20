package com.unilus.careconnect;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.dto.BookAppointmentRequest;
import com.unilus.careconnect.entity.Appointment;
import com.unilus.careconnect.service.AppointmentService;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(
            Authentication authentication,
            @Valid @RequestBody BookAppointmentRequest request) {

        try {
            Appointment appointment = appointmentService.bookAppointment(
                    authentication.getName(),
                    request.getAvailabilitySlotId(),
                    request.getSupportCategoryCode(),
                    request.getCounsellingMode(),
                    request.getAlternativeModeReason()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ResponseMapper.appointment(appointment)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMyAppointments(Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    appointmentService.getStudentAppointments(authentication.getName())
                            .stream()
                            .map(ResponseMapper::appointment)
                            .toList()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @PostMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(
            Authentication authentication,
            @PathVariable UUID appointmentId) {

        try {
            Appointment appointment = appointmentService.cancelAppointment(
                    authentication.getName(),
                    appointmentId
            );

            return ResponseEntity.ok(ResponseMapper.appointment(appointment));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
}