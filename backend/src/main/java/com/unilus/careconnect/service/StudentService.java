package com.unilus.careconnect.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
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

    public StudentService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Student registerStudent(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        validateInput(fullName, email, studentNumber, password);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedStudentNumber = studentNumber.trim();

        Integer emailCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """, Integer.class, normalizedEmail);

        if (emailCount != null && emailCount > 0) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        Integer studentNumberCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM credentials.student_registration_details
                WHERE lower(student_number) = lower(?)
                """, Integer.class, normalizedStudentNumber);

        if (studentNumberCount != null && studentNumberCount > 0) {
            throw new IllegalArgumentException(
                    "This student number is already registered.");
        }

        UUID userId = UUID.randomUUID();
        UUID studentProfileId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        String passwordHash = passwordEncoder.encode(password);

        try {
            jdbcTemplate.update("""
                    INSERT INTO credentials.users (
                        id, email, password_hash, enabled, email_verified, created_at
                    )
                    VALUES (?, ?, ?, true, true, ?)
                    """, userId, normalizedEmail, passwordHash, createdAt);

            jdbcTemplate.update("""
                    INSERT INTO wellness.student_profiles (id, created_at)
                    VALUES (?, ?)
                    """, studentProfileId, createdAt);

            jdbcTemplate.update("""
                    INSERT INTO credentials.student_identity_links (
                        user_id, student_profile_id, created_at
                    )
                    VALUES (?, ?, ?)
                    """, userId, studentProfileId, createdAt);

            jdbcTemplate.update("""
                    INSERT INTO credentials.student_registration_details (
                        user_id, full_name, student_number, created_at
                    )
                    VALUES (?, ?, ?, ?)
                    """, userId, fullName.trim(), normalizedStudentNumber, createdAt);

            Short studentRoleId = jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM credentials.roles
                    WHERE name = 'STUDENT'
                    """, Short.class);

            if (studentRoleId == null) {
                throw new IllegalStateException("STUDENT role is not configured.");
            }

            jdbcTemplate.update("""
                    INSERT INTO credentials.user_roles (user_id, role_id)
                    VALUES (?, ?)
                    """, userId, studentRoleId);


        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException(
                    "The student account could not be created.");
        }

        Student student = new Student();
        student.setId(studentProfileId);
        student.setCreatedAt(createdAt);
        return student;
    }


    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentProfile(String email) {
        try {
            return jdbcTemplate.queryForMap("""
                    SELECT u.email,
                           sr.full_name AS "fullName",
                           sr.student_number AS "studentNumber",
                           sp.created_at AS "createdAt"
                    FROM credentials.users u
                    JOIN credentials.student_identity_links sil ON sil.user_id = u.id
                    JOIN wellness.student_profiles sp ON sp.id = sil.student_profile_id
                    JOIN credentials.student_registration_details sr ON sr.user_id = u.id
                    WHERE lower(u.email) = lower(?)
                    """, email);
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("No student profile is linked to this account.");
        }
    }

    private void validateInput(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (fullName.trim().length() > 150) {
            throw new IllegalArgumentException("Full name cannot exceed 150 characters.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() > 254
                || !normalizedEmail.matches(
                        "^[a-z0-9.!#$%&'*+/=?^`{|}~-]+@(?:[a-z0-9-]+\\.)*unilus\\.ac\\.zm$")) {
            throw new IllegalArgumentException(
                    "Please use a valid university email address, including student subdomains.");
        }

        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException("Student number is required.");
        }
        if (studentNumber.trim().length() > 50) {
            throw new IllegalArgumentException("Student number cannot exceed 50 characters.");
        }

        PasswordPolicy.validate(password);
    }
}
