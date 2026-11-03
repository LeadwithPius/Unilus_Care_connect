package com.unilus.careconnect;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.service.ConsentService;
import com.unilus.careconnect.entity.ConsentDocument;

@RestController
@RequestMapping("/api/consents")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentDocument(
            @RequestParam(defaultValue = "COUNSELLING") String purposeCode) {
        try {
            ConsentDocument document = consentService.getCurrentDocument(purposeCode);
            return ResponseEntity.ok(ResponseMapper.consentDocument(document));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> recordDecision(
            Authentication authentication,
            @RequestBody ConsentDecisionRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("Consent decision is required.");
            }
            consentService.recordDecision(
                    authentication.getName(),
                    request.consentDocumentId(),
                    request.decision());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Your consent decision has been recorded."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record ConsentDecisionRequest(UUID consentDocumentId, String decision) {}
}
