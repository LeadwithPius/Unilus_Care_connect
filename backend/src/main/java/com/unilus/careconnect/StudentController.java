
package com.unilus.careconnect;

import com.unilus.careconnect.dto.StudentRegistrationRequest;
import com.unilus.careconnect.entity.UserAccount;
import com.unilus.careconnect.service.StudentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            UserAccount user = studentService.registerStudent(
                    request.getFullName(),
                    request.getEmail(),
                    request.getStudentNumber(),
                    request.getPassword()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Student registered successfully!",
                            "userId", user.getId(),
                            "email", user.getEmail()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
} 