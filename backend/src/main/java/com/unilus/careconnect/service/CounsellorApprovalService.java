
package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.CounsellorProfile;
import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.entity.UserRole;
import com.unilus.careconnect.repository.CounsellorProfileRepository;
import com.unilus.careconnect.repository.UserAccountRepository;
import com.unilus.careconnect.repository.UserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounsellorApprovalService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final CounsellorProfileRepository counsellorProfileRepository;

    public CounsellorApprovalService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            CounsellorProfileRepository counsellorProfileRepository) {

        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.counsellorProfileRepository = counsellorProfileRepository;
    }

    @Transactional
    public CounsellorProfile approveCounsellor(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "A valid counsellor user ID is required.");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "The requested user account was not found."));

        CounsellorProfile profile =
                counsellorProfileRepository.findByUserId(userId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "The counsellor profile was not found."));

        if (!"PENDING".equals(user.getAccountStatus())
                || !"PENDING".equals(profile.getAccountStatus())) {
            throw new IllegalStateException(
                    "Only pending counsellor accounts can be approved.");
        }

        user.setAccountStatus("ACTIVE");
        userAccountRepository.save(user);

        profile.setAccountStatus("ACTIVE");
        profile = counsellorProfileRepository.save(profile);

        boolean roleExists =
                userRoleRepository.existsByUserIdAndRoleName(
                        userId, "COUNSELLOR");

        if (!roleExists) {
            UserRole role = new UserRole();
            role.setUserId(userId);
            role.setRoleName("COUNSELLOR");
            userRoleRepository.save(role);
        }

        return profile;
    }
} 