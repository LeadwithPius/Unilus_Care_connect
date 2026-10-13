package com.unilus.careconnect;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Lets the frontend check a login and see which portal to open.
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(
                Map.of(
                        "email", authentication.getName(),
                        "roles", authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .map(role -> role.replaceFirst("^ROLE_", ""))
                                .toList()
                )
        );
    }
}