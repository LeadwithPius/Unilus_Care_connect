

package com.unilus.careconnect.service;

import com.unilus.careconnect.entity.Student;
import com.unilus.careconnect.repository.StudentRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Student registerStudent(
            String fullName,
            String email,
            String studentNumber,
            String password) {

        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "This email is already registered.");
        }

        if (studentRepository.existsByStudentNumber(studentNumber)) {
            throw new IllegalArgumentException(
                    "This student number is already registered.");
        }

        Student student = new Student();

        student.setFullName(fullName);
        student.setEmail(email);
        student.setStudentNumber(studentNumber);

        // Hash the password before saving it.
        student.setPasswordHash(passwordEncoder.encode(password));

        return studentRepository.save(student);
    }
} 