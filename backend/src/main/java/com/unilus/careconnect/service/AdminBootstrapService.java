package com.unilus.careconnect.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminBootstrapService.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {

        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates the initial administrator only when the configured
     * email does not already belong to an account.
     *
     * Security rules:
     * - The password is stored only as a BCrypt hash.
     * - Existing accounts are never automatically promoted.
     * - Existing administrator passwords are never overwritten.
     * - The operation is transactional.
     */
    @Transactional
    public void bootstrapAdmin(
            String suppliedEmail,
            String rawPassword) {

        String email = normalizeEmail(suppliedEmail);

        validatePassword(rawPassword);

        List<UUID> existingUsers = jdbcTemplate.query(
                """
                SELECT id
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """,
                (rs, rowNum) ->
                        rs.getObject("id", UUID.class),
                email
        );

        if (!existingUsers.isEmpty()) {

            UUID existingUserId = existingUsers.get(0);

            Integer adminRoleCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM credentials.user_roles ur
                    JOIN credentials.roles r
                      ON r.id = ur.role_id
                    WHERE ur.user_id = ?
                      AND r.name = 'ADMIN'
                    """,
                    Integer.class,
                    existingUserId
            );

            if (adminRoleCount != null && adminRoleCount > 0) {

                LOGGER.info(
                        "Administrator account is already provisioned. "
                                + "No account changes were made."
                );

                return;
            }

            throw new IllegalStateException(
                    "The configured administrator email already belongs "
                            + "to an account without the ADMIN role. "
                            + "Bootstrap will not promote that account. "
                            + "Use a dedicated administrator email or "
                            + "perform a separately authorized promotion."
            );
        }

        List<Short> adminRoleIds = jdbcTemplate.query(
                """
                SELECT id
                FROM credentials.roles
                WHERE name = 'ADMIN'
                """,
                (rs, rowNum) -> rs.getShort("id")
        );

        if (adminRoleIds.isEmpty()) {
            throw new IllegalStateException(
                    "The ADMIN role does not exist. "
                            + "Check the database migrations before "
                            + "provisioning the administrator."
            );
        }

        Short adminRoleId = adminRoleIds.get(0);

        UUID userId = UUID.randomUUID();

        String passwordHash = passwordEncoder.encode(rawPassword);

        jdbcTemplate.update(
                """
                INSERT INTO credentials.users (
                    id,
                    email,
                    password_hash,
                    enabled,
                    email_verified
                )
                VALUES (?, ?, ?, TRUE, TRUE)
                """,
                userId,
                email,
                passwordHash
        );

        jdbcTemplate.update(
                """
                INSERT INTO credentials.user_roles (
                    user_id,
                    role_id
                )
                VALUES (?, ?)
                """,
                userId,
                adminRoleId
        );

        LOGGER.info(
                "Initial administrator account was provisioned successfully."
        );
    }

    private String normalizeEmail(String suppliedEmail) {

        if (suppliedEmail == null || suppliedEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator email is required."
            );
        }

        String email = suppliedEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        if (email.length() > 254
                || !email.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new IllegalArgumentException(
                    "A valid administrator email address is required."
            );
        }

        return email;
    }

    private void validatePassword(String password) {

        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException(
                    "The initial administrator password must contain "
                            + "at least 12 characters."
            );
        }
    }
}