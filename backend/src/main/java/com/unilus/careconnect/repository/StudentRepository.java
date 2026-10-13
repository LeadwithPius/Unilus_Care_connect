package com.unilus.careconnect.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.unilus.careconnect.entity.Student;
public interface StudentRepository
        extends JpaRepository<Student, UUID> {
}