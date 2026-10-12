package com.unilus.careconnect;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CounsellorService counsellorService;
    private final ConsentService consentService;

    public AdminController(
            CounsellorService counsellorService,
            ConsentService consentService) {
        this.counsellorService = counsellorService;
        this.consentService = consentService;
    }

    @PostMapping("/counsellors")
    public ResponseEntity<?> createCounsellor(
            @RequestBody CreateCounsellorRequest request) {

        try {
            CounsellorProfile profile = counsellorService.createCounsellor(
                    request.getEmail(),
                    request.getPassword(),
                    request.getDisplayName(),
                    request.getProfessionalTitle(),
                    request.getBiography()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ResponseMapper.counsellor(profile)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
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
                    request.getBody()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ResponseMapper.consentDocument(document)
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
}