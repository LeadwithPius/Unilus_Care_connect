
package com.unilus.careconnect.repository;

import com.unilus.careconnect.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository
        extends JpaRepository<UserRole, Long> {

    boolean existsByUserIdAndRoleName(
            Long userId,
            String roleName);

    List<UserRole> findAllByUserId(Long userId);
} 