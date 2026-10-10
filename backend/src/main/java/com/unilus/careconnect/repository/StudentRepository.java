
package com.unilus.careconnect.repository;
import com.unilus.careconnect.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StudentRepository extends JpaRepository<Student, Long> {
Optional<Student> findByEmail(String email);

Optional<Student> findByStudentNumber(String studentNumber);

boolean existsByEmail(String email);

boolean existsByStudentNumber(String studentNumber);
}
