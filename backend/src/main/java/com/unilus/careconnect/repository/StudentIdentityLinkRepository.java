
package com.unilus.careconnect.repository;

import com.unilus.careconnect.entity.StudentIdentityLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentIdentityLinkRepository
        extends JpaRepository<StudentIdentityLink, Long> {

    Optional<StudentIdentityLink> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
} 