package com.unilus.careconnect;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import com.unilus.careconnect.dto.LearningProgressRequest;
import com.unilus.careconnect.service.EducationService;

@RestController
public class EducationController {

    private final EducationService educationService;

    public EducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping("/api/education")
    public ResponseEntity<?> listEducation(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, name = "q") String search) {
        return ResponseEntity.ok(educationService.listPublished(topic, search));
    }

    @GetMapping("/api/game/progress")
    public ResponseEntity<?> getLearningProgress(Authentication authentication) {
        try {
            return ResponseEntity.ok(educationService.getProgress(authentication.getName()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/api/game/progress")
    public ResponseEntity<?> saveLearningProgress(
            Authentication authentication,
            @Valid @RequestBody LearningProgressRequest request) {
        try {
            return ResponseEntity.ok(educationService.saveProgress(
                    authentication.getName(), request.getSection(), request.getScore()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }
}
