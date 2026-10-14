
package com.unilus.careconnect.controller;

import com.unilus.careconnect.dto.CounsellorRegistrationRequest;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.service.CounsellorService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/counsellors")
public class CounsellorController {

    private final CounsellorService counsellorService;

    public CounsellorController(CounsellorService counsellorService) {
        this.counsellorService = counsellorService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerCounsellor(
            @RequestBody CounsellorRegistrationRequest request) {

        try {
            CounsellorProfile profile =
                    counsellorService.registerCounsellor(request);

            Map<String, Object> response = Map.of(
                    "message",
                    "Counsellor registration submitted successfully. Your account is pending approval.",
                    "profileId",
                    profile.getId(),
                    "status",
                    profile.getAccountStatus()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            exception.getMessage()
                    ));

        } catch (IllegalStateException exception) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            exception.getMessage()
                    ));

        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "An account with these details may already exist."
                    ));
        }
    }
} 