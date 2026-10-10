package com.unilus.careconnect;

import com.unilus.careconnect.dto.StudentRegistrationRequest;
import com.unilus.careconnect.entity.Student;
import com.unilus.careconnect.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerStudent(
            @RequestBody StudentRegistrationRequest request) {

        try {
            Student student = studentService.registerStudent(
                    request.getFullName(),
                    request.getEmail(),
                    request.getStudentNumber(),
                    request.getPassword()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Student registered successfully!",
                            "studentId", student.getId(),
                            "fullName", student.getFullName(),
                            "email", student.getEmail(),
                            "studentNumber", student.getStudentNumber()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }
} 