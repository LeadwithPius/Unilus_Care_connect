package com.unilus.careconnect.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "User ID is required.")
        String userId,

        @NotBlank(message = "Password is required.")
        String password,

        String role
) {
}