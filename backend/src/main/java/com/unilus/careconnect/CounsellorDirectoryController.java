
package com.unilus.careconnect;

import com.unilus.careconnect.service.CounsellorDirectoryService;
import com.unilus.careconnect.service.CounsellorDirectoryService.CounsellorDirectoryEntry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/counsellors")
public class CounsellorDirectoryController {

    private final CounsellorDirectoryService directoryService;

    public CounsellorDirectoryController(
            CounsellorDirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public ResponseEntity<List<CounsellorDirectoryEntry>> getAvailableCounsellors() {
        List<CounsellorDirectoryEntry> counsellors =
                directoryService.getAvailableCounsellors();

        return ResponseEntity.ok(counsellors);
    }
} 