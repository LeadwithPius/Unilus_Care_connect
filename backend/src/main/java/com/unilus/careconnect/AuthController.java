package com.unilus.careconnect;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.dto.ForgotPasswordRequest;
import com.unilus.careconnect.dto.ResetPasswordRequest;
import com.unilus.careconnect.dto.VerifyEmailRequest;
import com.unilus.careconnect.service.EmailVerificationService;
import com.unilus.careconnect.service.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthController.class);

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

  public AuthController(
        PasswordResetService passwordResetService,
        EmailVerificationService emailVerificationService) {

    this.passwordResetService = passwordResetService;
    this.emailVerificationService = emailVerificationService;
}

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication) {

        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "roles", authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .map(role -> role.replaceFirst("^ROLE_", ""))
                                .toList()
                )
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        try {
            passwordResetService.requestPasswordReset(request.email());
        } catch (RuntimeException exception) {
            LOGGER.error("Password reset request failed.", exception);
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "If an eligible account exists for that email, a password-reset link will be sent."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        boolean reset = passwordResetService.resetPassword(
                request.token(),
                request.newPassword());

        if (!reset) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message",
                    "The reset link is invalid or has expired. Please request a new one."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "Your password has been reset successfully."
        ));
    }

    @PostMapping("/verify-email")
public ResponseEntity<Map<String, String>> verifyEmail(
        @Valid @RequestBody VerifyEmailRequest request) {

    boolean verified = emailVerificationService.verifyEmail(
            request.token());

    if (!verified) {
        return ResponseEntity.badRequest().body(Map.of(
                "message",
                "The verification link is invalid, expired, or already used."
        ));
    }

    return ResponseEntity.ok(Map.of(
            "message",
            "Your email has been verified. You can now log in."
    ));
}
}