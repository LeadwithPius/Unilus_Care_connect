package com.unilus.careconnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${careconnect.email-verification.expiry-minutes:30}")
    private long expiryMinutes;

    @Value("${careconnect.frontend.verification-url:http://localhost:5173/verify-email}")
    private String verificationUrl;

    public EmailVerificationService(
            JdbcTemplate jdbcTemplate,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder) {

        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a verification token and sends the verification email.
     *
     * This method participates in the registration transaction.
     * A mail delivery failure is propagated so registration can roll back.
     */
    @Transactional
    public void sendVerificationEmail(UUID userId, String recipient) {

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = sha256Hex(rawToken);

        // Invalidate previously issued verification links.
        jdbcTemplate.update("""
                UPDATE credentials.email_verification_tokens
                SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND used_at IS NULL
                """,
                userId);

        jdbcTemplate.update("""
                INSERT INTO credentials.email_verification_tokens (
                    id,
                    user_id,
                    token_hash,
                    created_at,
                    expires_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP + (? * INTERVAL '1 minute')
                )
                """,
                UUID.randomUUID(),
                userId,
                tokenHash,
                expiryMinutes);

        String separator = verificationUrl.contains("?") ? "&" : "?";

        String verificationLink = verificationUrl
                + separator
                + "token="
                + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailUsername);
        message.setTo(recipient);
        message.setSubject("Verify your CareConnect email address");

        message.setText("""
                Hello,

                Thank you for registering with Unilus Care Connect.

                Please open the following link to verify your email address:

                %s

                This link expires in %d minutes and can only be used once.

                If you did not create this account, you can ignore this email.

                Unilus Care Connect Support
                """.formatted(verificationLink, expiryMinutes));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException(
                    "Unable to send the verification email.",
                    exception);
        }
    }

    /**
     * Validates a verification token and activates the account.
     */
    @Transactional
    public boolean verifyEmail(String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()
                || rawToken.length() > 100) {
            return false;
        }

        String tokenHash = sha256Hex(rawToken);

        List<VerificationToken> tokens = jdbcTemplate.query("""
                SELECT id, user_id
                FROM credentials.email_verification_tokens
                WHERE token_hash = ?
                  AND used_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                FOR UPDATE
                """,
                (rs, rowNum) -> new VerificationToken(
                        rs.getObject("id", UUID.class),
                        rs.getObject("user_id", UUID.class)),
                tokenHash);

        if (tokens.isEmpty()) {
            return false;
        }

        VerificationToken token = tokens.get(0);

        // Activate the account only after a valid token is supplied.
        int updatedUsers = jdbcTemplate.update("""
                UPDATE credentials.users
                SET email_verified = TRUE,
                    enabled = TRUE
                WHERE id = ?
                  AND email_verified = FALSE
                """,
                token.userId());

        if (updatedUsers != 1) {
            return false;
        }

        // Consume the token and invalidate other outstanding links.
        jdbcTemplate.update("""
                UPDATE credentials.email_verification_tokens
                SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND used_at IS NULL
                """,
                token.userId());

        return true;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.",
                    exception);
        }
    }

    private record VerificationToken(UUID id, UUID userId) {}
}