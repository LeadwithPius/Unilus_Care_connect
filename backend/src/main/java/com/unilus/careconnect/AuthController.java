package com.unilus.careconnect;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unilus.careconnect.dto.ForgotPasswordRequest;
import com.unilus.careconnect.dto.LoginRequest;
import com.unilus.careconnect.dto.ResetPasswordRequest;
import com.unilus.careconnect.dto.VerifyEmailRequest;
import com.unilus.careconnect.service.AuthService;
import com.unilus.careconnect.service.EmailVerificationService;
import com.unilus.careconnect.service.PasswordResetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService,
            EmailVerificationService emailVerificationService) {

        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            Map<String, Object> response = authService.login(
                    request.userId(),
                    request.password(),
                    request.role()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error",
                            "Invalid user ID, password, or role. Check that your account is active and your email is verified."
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication) {

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(role -> role.substring("ROLE_".length()))
                .distinct()
                .sorted()
                .toList();

        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "roles", roles
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
                request.newPassword()
        );

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
                request.token()
        );

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