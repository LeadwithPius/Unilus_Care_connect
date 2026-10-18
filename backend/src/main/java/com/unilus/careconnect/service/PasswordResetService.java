package com.unilus.careconnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${careconnect.password-reset.expiry-minutes:15}")
    private long expiryMinutes;

    @Value("${careconnect.password-reset.base-url:http://localhost:5173/reset-password}")
    private String resetBaseUrl;

    public PasswordResetService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    /**
     * Requests a reset link.
     * The caller should always return the same public response,
     * regardless of whether the email belongs to an account.
     */
    @Transactional
    public void requestPasswordReset(String suppliedEmail) {
        String email = suppliedEmail.trim();

        List<ResetAccount> accounts = jdbcTemplate.query("""
                SELECT id, email
                FROM credentials.users
                WHERE lower(email) = lower(?)
                  AND enabled = TRUE
                  AND email_verified = TRUE
                """,
                (rs, rowNum) -> new ResetAccount(
                        rs.getObject("id", UUID.class),
                        rs.getString("email")),
                email);

        // Do not reveal whether an account exists.
        if (accounts.isEmpty()) {
            return;
        }

        ResetAccount account = accounts.get(0);

        // Generate a cryptographically secure, URL-safe token.
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = sha256Hex(rawToken);

        // Invalidate any previous unused reset links for this account.
        jdbcTemplate.update("""
                UPDATE credentials.password_reset_tokens
                SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND used_at IS NULL
                """,
                account.id());

        // Store only the token hash, never the raw token.
        jdbcTemplate.update("""
                INSERT INTO credentials.password_reset_tokens
                    (user_id, token_hash, expires_at)
                VALUES (?, ?, CURRENT_TIMESTAMP + (? * INTERVAL '1 minute'))
                """,
                account.id(),
                tokenHash,
                expiryMinutes);

        sendResetEmail(account.email(), rawToken);
    }

    /**
     * Validates the token and changes the password atomically.
     */
    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = sha256Hex(rawToken);

        List<ResetToken> tokens = jdbcTemplate.query("""
                SELECT id, user_id
                FROM credentials.password_reset_tokens
                WHERE token_hash = ?
                  AND used_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                FOR UPDATE
                """,
                (rs, rowNum) -> new ResetToken(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class)),
                tokenHash);

        if (tokens.isEmpty()) {
            return false;
        }

        ResetToken token = tokens.get(0);
        String encodedPassword = passwordEncoder.encode(newPassword);

        int updatedUsers = jdbcTemplate.update("""
                UPDATE credentials.users
                SET password_hash = ?
                WHERE id = ?
                """,
                encodedPassword,
                token.userId());

        if (updatedUsers != 1) {
            return false;
        }

        // Mark the used token and invalidate all other outstanding tokens.
        jdbcTemplate.update("""
                UPDATE credentials.password_reset_tokens
                SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND used_at IS NULL
                """,
                token.userId());

        return true;
    }

    private void sendResetEmail(String recipient, String rawToken) {
        String resetLink = resetBaseUrl
                + (resetBaseUrl.contains("?") ? "&" : "?")
                + "token="
                + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(recipient);
        message.setSubject("CareConnect password reset");
        message.setText("""
                Hello,

                We received a request to reset your CareConnect password.

                Use the link below to choose a new password:

                %s

                This link expires in %d minutes and can only be used once.

                If you did not request this change, you can ignore this email.

                CareConnect Support
                """.formatted(resetLink, expiryMinutes));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            // Throwing a runtime exception rolls back the token transaction.
            throw new IllegalStateException(
                    "Unable to send password reset email.", exception);
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", exception);
        }
    }

    private record ResetAccount(UUID id, String email) {}

    private record ResetToken(UUID id, UUID userId) {}
}