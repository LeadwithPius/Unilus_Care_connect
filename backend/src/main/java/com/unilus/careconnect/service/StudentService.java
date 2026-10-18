package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unilus.careconnect.entity.Student;

@Service
public class StudentService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public StudentService(
        JdbcTemplate jdbcTemplate,
        PasswordEncoder passwordEncoder,
        EmailVerificationService emailVerificationService) {

    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
}

    @Transactional
    public Student registerStudent(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        validateInput(fullName, email, studentNumber, password);

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedStudentNumber = studentNumber.trim();

        // ---------------------------------------------------------
        // Prevent duplicate email
        // ---------------------------------------------------------

        Integer emailCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """,
                Integer.class,
                normalizedEmail);

        if (emailCount != null && emailCount > 0) {
            throw new IllegalArgumentException(
                    "This email is already registered.");
        }

        // ---------------------------------------------------------
        // Prevent duplicate student number
        // ---------------------------------------------------------

        Integer studentNumberCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM credentials.student_registration_details
                WHERE lower(student_number) = lower(?)
                """,
                Integer.class,
                normalizedStudentNumber);

        if (studentNumberCount != null && studentNumberCount > 0) {
            throw new IllegalArgumentException(
                    "This student number is already registered.");
        }

        UUID userId = UUID.randomUUID();
        UUID studentProfileId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        String passwordHash = passwordEncoder.encode(password);

        try {

            // -----------------------------------------------------
            // 1. Create authentication account
            // -----------------------------------------------------

           jdbcTemplate.update("""
        INSERT INTO credentials.users (
            id,
            email,
            password_hash,
            enabled,
            email_verified,
            created_at
        )
        VALUES (?, ?, ?, false, false, ?)
        """,
        userId,
        normalizedEmail,
        passwordHash,
        createdAt);

        emailVerificationService.sendVerificationEmail(userId,normalizedEmail);

            // -----------------------------------------------------
            // 2. Create wellness student profile
            // -----------------------------------------------------

            jdbcTemplate.update("""
                    INSERT INTO wellness.student_profiles (
                        id,
                        created_at
                    )
                    VALUES (?, ?)
                    """,
                    studentProfileId,
                    createdAt);

            // -----------------------------------------------------
            // 3. Link account to student profile
            // -----------------------------------------------------

            jdbcTemplate.update("""
                    INSERT INTO credentials.student_identity_links (
                        user_id,
                        student_profile_id,
                        created_at
                    )
                    VALUES (?, ?, ?)
                    """,
                    userId,
                    studentProfileId,
                    createdAt);

            // -----------------------------------------------------
            // 4. Store registration information
            // -----------------------------------------------------

            jdbcTemplate.update("""
                    INSERT INTO credentials.student_registration_details (
                        user_id,
                        full_name,
                        student_number,
                        created_at
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    userId,
                    fullName.trim(),
                    normalizedStudentNumber,
                    createdAt);

            // -----------------------------------------------------
            // 5. Assign STUDENT role
            // -----------------------------------------------------

            Short studentRoleId = jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM credentials.roles
                    WHERE name = 'STUDENT'
                    """,
                    Short.class);

            if (studentRoleId == null) {
                throw new IllegalStateException(
                        "STUDENT role is not configured.");
            }

            jdbcTemplate.update("""
                    INSERT INTO credentials.user_roles (
                        user_id,
                        role_id
                    )
                    VALUES (?, ?)
                    """,
                    userId,
                    studentRoleId);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "The student account could not be created.");
        }

        Student student = new Student();

        student.setId(studentProfileId);
        student.setCreatedAt(createdAt);

        return student;
    }

    private void validateInput(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (fullName.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 150 characters.");
        }

      if (email == null || email.isBlank()) {
    throw new IllegalArgumentException(
            "Email is required.");
}

String normalizedEmail = email.trim().toLowerCase(
        java.util.Locale.ROOT);

if (normalizedEmail.length() > 254
        || !normalizedEmail.matches(
                "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@unilus\\.ac\\.zm$")) {

    throw new IllegalArgumentException(
            "Please use a valid @unilus.ac.zm university email address.");
}

        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Student number is required.");
        }

        if (studentNumber.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Student number cannot exceed 50 characters.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }
    }
}