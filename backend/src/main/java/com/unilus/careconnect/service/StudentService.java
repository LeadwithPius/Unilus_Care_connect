
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.entity.UserRole;
import com.unilus.careconnect.entity.StudentProfile;
import com.unilus.careconnect.entity.StudentIdentityLink;
import com.unilus.careconnect.entity.StudentRegistrationDetails;

import com.unilus.careconnect.repository.UserAccountRepository;
import com.unilus.careconnect.repository.UserRoleRepository;
import com.unilus.careconnect.repository.StudentProfileRepository;
import com.unilus.careconnect.repository.StudentIdentityLinkRepository;
import com.unilus.careconnect.repository.StudentRegistrationDetailsRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentIdentityLinkRepository studentIdentityLinkRepository;
    private final StudentRegistrationDetailsRepository studentRegistrationDetailsRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public StudentService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            StudentProfileRepository studentProfileRepository,
            StudentIdentityLinkRepository studentIdentityLinkRepository,
            StudentRegistrationDetailsRepository studentRegistrationDetailsRepository) {

        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.studentIdentityLinkRepository = studentIdentityLinkRepository;
        this.studentRegistrationDetailsRepository =
                studentRegistrationDetailsRepository;
    }

    @Transactional
    public UserAccount registerStudent(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        // 1. Validate the registration details.
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (fullName.trim().length() > 150) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 150 characters.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email address is required.");
        }

        email = email.trim().toLowerCase();

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address.");
        }

        if (studentNumber == null || studentNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Student number is required.");
        }

        studentNumber = studentNumber.trim();

        if (studentNumber.length() > 50) {
            throw new IllegalArgumentException(
                    "Student number must not exceed 50 characters.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        // 2. Check for duplicate accounts.
        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "An account with this email already exists.");
        }

        if (studentRegistrationDetailsRepository
                .existsByStudentNumber(studentNumber)) {
            throw new IllegalStateException(
                    "This student number is already registered.");
        }

        // 3. Save the account in credentials.users.
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        user = userAccountRepository.saveAndFlush(user);

        Long userId = user.getId();

        // 4. Save the role in credentials.user_roles.
        UserRole role = new UserRole();
        role.setUserId(userId);
        role.setRoleName("STUDENT");

        userRoleRepository.save(role);

        // 5. Create the profile in wellness.student_profiles.
        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);

        studentProfileRepository.save(profile);

        // 6. Create the identity link.
        StudentIdentityLink identityLink =
                new StudentIdentityLink();

        identityLink.setUserId(userId);

        studentIdentityLinkRepository.save(identityLink);

        // 7. Save the name and student number.
        StudentRegistrationDetails details =
                new StudentRegistrationDetails();

        details.setUserId(userId);
        details.setFullName(fullName.trim());
        details.setStudentNumber(studentNumber);

        studentRegistrationDetailsRepository.save(details);

        // Return the account, not the password.
        return user;
    }
} 