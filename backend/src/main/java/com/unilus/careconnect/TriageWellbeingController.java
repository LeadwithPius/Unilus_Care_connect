package com.unilus.careconnect;

import com.unilus.careconnect.service.TriageWellbeingService;
import com.unilus.careconnect.service.TriageWellbeingService.CheckInRequest;
import com.unilus.careconnect.service.TriageWellbeingService.TriageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/triage")
public class TriageWellbeingController {
    private final TriageWellbeingService service;

    public TriageWellbeingController(TriageWellbeingService service) {
        this.service = service;
    }

    @PostMapping("/assessments")
    public ResponseEntity<?> submitTriage(Authentication auth, @RequestBody TriageRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.submitTriage(auth.getName(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/assessments/mine")
    public ResponseEntity<?> myTriage(Authentication auth) {
        try {
            return ResponseEntity.ok(service.myTriageHistory(auth.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/questionnaire")
    public ResponseEntity<?> questionnaire() {
        return ResponseEntity.ok(service.activeQuestionnaire());
    }

    @PostMapping("/check-ins")
    public ResponseEntity<?> submitCheckIn(Authentication auth, @RequestBody CheckInRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.submitCheckIn(auth.getName(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check-ins/mine")
    public ResponseEntity<?> myCheckIns(Authentication auth) {
        try {
            return ResponseEntity.ok(service.myCheckIns(auth.getName()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/progress/mine")
    public ResponseEntity<?> myProgress(Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of(
                    "triageAssessments", service.myTriageHistory(auth.getName()),
                    "wellbeingCheckIns", service.myCheckIns(auth.getName()),
                    "disclaimer", "Self-reported reflections are not a diagnosis or proof that a person is safe."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/counsellor/students/{studentProfileId}/progress")
    public ResponseEntity<?> counsellorProgress(Authentication auth, @PathVariable UUID studentProfileId) {
        try {
            return ResponseEntity.ok(service.counsellorCheckIns(auth.getName(), studentProfileId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
