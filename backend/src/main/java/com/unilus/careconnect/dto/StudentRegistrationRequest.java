package com.unilus.careconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StudentRegistrationRequest {

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 254)
    @Pattern(
        regexp = "^[^@\\s]+@unilus\\.ac\\.zm$",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "Please use your @unilus.ac.zm university email address."
    )
    private String email;

    @NotBlank
    @Size(max = 50)
    private String studentNumber;

    @NotBlank
    @Size(min = 12, max = 72)
    private String password;

    public StudentRegistrationRequest() {}

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}