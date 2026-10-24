package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unilus.careconnect.entity.CounsellorProfile;

@Service
public class CounsellorService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public CounsellorService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public CounsellorProfile createCounsellor(
            String email,
            String password,
            String displayName,
            String professionalTitle,
            String biography) {

        validateInput(email, password, displayName, professionalTitle, biography);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Integer existingUserCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """, Integer.class, normalizedEmail);

        if (existingUserCount != null && existingUserCount > 0) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }

        UUID userId = UUID.randomUUID();
        UUID counsellorProfileId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        String passwordHash = passwordEncoder.encode(password);

        try {
            jdbcTemplate.update("""
                    INSERT INTO credentials.users (
                        id, email, password_hash, enabled, email_verified, created_at
                    )
                    VALUES (?, ?, ?, false, false, ?)
                    """, userId, normalizedEmail, passwordHash, createdAt);

            Short counsellorRoleId = jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM credentials.roles
                    WHERE name = 'COUNSELLOR'
                    """, Short.class);

            if (counsellorRoleId == null) {
                throw new IllegalStateException("COUNSELLOR role is not configured.");
            }

            jdbcTemplate.update("""
                    INSERT INTO credentials.user_roles (user_id, role_id)
                    VALUES (?, ?)
                    """, userId, counsellorRoleId);

            jdbcTemplate.update("""
                    INSERT INTO credentials.counsellor_profiles (
                        id, user_id, display_name, professional_title, biography,
                        accepting_appointments, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, false, ?)
                    """,
                    counsellorProfileId,
                    userId,
                    displayName.trim(),
                    normalizeNullable(professionalTitle),
                    normalizeNullable(biography),
                    createdAt);

            // Counsellor accounts remain disabled until their email is verified.
            emailVerificationService.sendVerificationEmail(userId, normalizedEmail);

        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "The counsellor account could not be created.");
        }

        CounsellorProfile profile = new CounsellorProfile();
        profile.setId(counsellorProfileId);
        profile.setUserId(userId);
        profile.setDisplayName(displayName.trim());
        profile.setProfessionalTitle(normalizeNullable(professionalTitle));
        profile.setBiography(normalizeNullable(biography));
        profile.setAcceptingAppointments(false);
        profile.setCreatedAt(createdAt);
        return profile;
    }

    private void validateInput(
            String email,
            String password,
            String displayName,
            String professionalTitle,
            String biography) {

        if (email == null || email.isBlank()
                || email.length() > 254
                || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }

        PasswordPolicy.validate(password);

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name is required.");
        }
        if (displayName.trim().length() > 150) {
            throw new IllegalArgumentException("Display name cannot exceed 150 characters.");
        }
        if (professionalTitle != null && professionalTitle.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Professional title cannot exceed 150 characters.");
        }
        if (biography != null && biography.trim().length() > 5000) {
            throw new IllegalArgumentException("Biography cannot exceed 5000 characters.");
        }
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
