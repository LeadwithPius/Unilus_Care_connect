package com.unilus.careconnect;

import com.unilus.careconnect.service.CounsellingChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class CounsellingChatController {

    private final CounsellingChatService chatService;

    public CounsellingChatController(CounsellingChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping({"/student/conversations", "/counsellor/conversations"})
    public ResponseEntity<?> conversations(Authentication authentication,
                                             jakarta.servlet.http.HttpServletRequest request) {
        String role = roleFromPath(request);
        try {
            return ResponseEntity.ok(
                    chatService.conversations(authentication.getName(), role));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping({
            "/student/conversations/{conversationId}/messages",
            "/counsellor/conversations/{conversationId}/messages"
    })
    public ResponseEntity<?> messages(Authentication authentication,
                                       @PathVariable UUID conversationId,
                                       jakarta.servlet.http.HttpServletRequest request) {
        String role = roleFromPath(request);
        try {
            return ResponseEntity.ok(chatService.messages(
                    authentication.getName(), role, conversationId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping({
            "/student/conversations/{conversationId}/messages",
            "/counsellor/conversations/{conversationId}/messages"
    })
    public ResponseEntity<?> sendMessage(Authentication authentication,
                                         @PathVariable UUID conversationId,
                                         @Valid @RequestBody SendMessageRequest request,
                                         jakarta.servlet.http.HttpServletRequest httpRequest) {
        String role = roleFromPath(httpRequest);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    chatService.sendMessage(authentication.getName(), role,
                            conversationId, request.content()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    private String roleFromPath(jakarta.servlet.http.HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/counsellor/") ? "COUNSELLOR" : "STUDENT";
    }

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String content) {}
}
