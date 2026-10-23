package com.unilus.careconnect.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Duration jwtLifetime;
    private final String jwtIssuer;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            Duration jwtLifetime,
            @Value("${careconnect.jwt.issuer:careconnect-api}")
            String jwtIssuer) {

        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtLifetime = jwtLifetime;
        this.jwtIssuer = jwtIssuer;
    }

    public Map<String, Object> login(
            String userId,
            String password,
            String requestedRole) {

        String identifier = userId == null ? "" : userId.trim();

        if (identifier.isBlank()
                || password == null
                || password.isBlank()) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                identifier,
                                password
                        )
                );

        // Keep only application roles, excluding authentication factors.
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .distinct()
                .sorted()
                .toList();

        validateRequestedRole(requestedRole, roles);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtLifetime);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        org.springframework.security.oauth2.jwt.JwsHeader
                                .with(MacAlgorithm.HS256)
                                .build(),
                        claims
                )
        ).getTokenValue();

        List<String> responseRoles = roles.stream()
                .map(role -> role.substring("ROLE_".length()))
                .toList();

        return Map.of(
                "token", token,
                "tokenType", "Bearer",
                "expiresIn", jwtLifetime.toSeconds(),
                "email", authentication.getName(),
                "roles", responseRoles
        );
    }

    private void validateRequestedRole(
            String requestedRole,
            List<String> actualRoles) {

        if (requestedRole == null || requestedRole.isBlank()) {
            return;
        }

        String normalizedRole = requestedRole.trim().toUpperCase();

        normalizedRole = switch (normalizedRole) {
            case "S" -> "STUDENT";
            case "C" -> "COUNSELLOR";
            case "A" -> "ADMIN";
            default -> normalizedRole;
        };

        String requiredAuthority = normalizedRole.startsWith("ROLE_")
                ? normalizedRole
                : "ROLE_" + normalizedRole;

        if (!actualRoles.contains(requiredAuthority)) {
            throw new BadCredentialsException("Invalid credentials.");
        }
    }
}