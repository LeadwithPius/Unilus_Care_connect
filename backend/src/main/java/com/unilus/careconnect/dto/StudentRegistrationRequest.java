
package com.unilus.careconnect.dto;
public class StudentRegistrationRequest {
private String fullName;
private String email;
private String studentNumber;
private String password;

public StudentRegistrationRequest() {
}

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
