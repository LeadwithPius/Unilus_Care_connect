
package com.unilus.careconnect.service;

import com.unilus.careconnect.dto.CounsellorRegistrationRequest;
import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.repository.CounsellorProfileRepository;
import com.unilus.careconnect.repository.UserAccountRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounsellorService {

    private final UserAccountRepository userAccountRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public CounsellorService(
            UserAccountRepository userAccountRepository,
            CounsellorProfileRepository counsellorProfileRepository) {

        this.userAccountRepository = userAccountRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
    }

    @Transactional
    public CounsellorProfile registerCounsellor(
            CounsellorRegistrationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Registration details are required.");
        }

        String fullName = request.getFullName();
        String email = request.getEmail();
        String password = request.getPassword();
        String professionalTitle = request.getProfessionalTitle();
        String biography = request.getBiography();

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        fullName = fullName.trim();

        if (fullName.length() > 150) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 150 characters.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email address is required.");
        }

        email = email.trim().toLowerCase();

        if (!email.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        if (professionalTitle != null
                && professionalTitle.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Professional title must not exceed 100 characters.");
        }

        if (biography != null && biography.length() > 5000) {
            throw new IllegalArgumentException(
                    "Biography must not exceed 5000 characters.");
        }

        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "An account with this email already exists.");
        }

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAccountStatus("PENDING");

        user = userAccountRepository.saveAndFlush(user);

        CounsellorProfile profile = new CounsellorProfile();
        profile.setUserId(user.getId());
        profile.setFullName(fullName);

        if (professionalTitle != null
                && !professionalTitle.isBlank()) {
            profile.setProfessionalTitle(
                    professionalTitle.trim());
        }

        if (biography != null && !biography.isBlank()) {
            profile.setBiography(biography.trim());
        }

        profile.setAccountStatus("PENDING");

        return counsellorProfileRepository.save(profile);
    }
} 