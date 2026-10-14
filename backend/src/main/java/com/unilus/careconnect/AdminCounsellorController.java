
package com.unilus.careconnect;

import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.service.CounsellorApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/counsellors")
public class AdminCounsellorController {

    private final CounsellorApprovalService service;

    public AdminCounsellorController(CounsellorApprovalService service) {
        this.service = service;
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<Map<String, Object>> approveCounsellor(
            @PathVariable Long userId) {
        try {
            CounsellorProfile profile = service.approveCounsellor(userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Counsellor approved successfully.",
                    "profileId", profile.getId(),
                    "userId", profile.getUserId(),
                    "status", profile.getAccountStatus()
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", exception.getMessage()
            ));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", exception.getMessage()
            ));
        }
    }
}