
package com.unilus.careconnect.repository;

import com.unilus.careconnect.entity.StudentRegistrationDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRegistrationDetailsRepository
        extends JpaRepository<StudentRegistrationDetails, Long> {

    Optional<StudentRegistrationDetails> findByStudentNumber(
            String studentNumber);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByUserId(Long userId);
} 