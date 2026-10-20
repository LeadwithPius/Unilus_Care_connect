package com.unilus.careconnect;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.unilus.careconnect.dto.EducationResourceRequest;
import com.unilus.careconnect.service.EducationService;

@RestController
@RequestMapping("/api/admin/education")
public class AdminEducationController {

    private final EducationService educationService;

    public AdminEducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping
    public ResponseEntity<?> listResources() {
        return ResponseEntity.ok(educationService.listAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<?> createResource(@Valid @RequestBody EducationResourceRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(educationService.createResource(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResource(
            @PathVariable UUID id,
            @Valid @RequestBody EducationResourceRequest request) {
        try {
            return ResponseEntity.ok(educationService.updateResource(id, request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PatchMapping("/{id}/publication")
    public ResponseEntity<?> setPublication(
            @PathVariable UUID id,
            @RequestParam boolean published) {
        try {
            return ResponseEntity.ok(educationService.setPublished(id, published));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }
}
