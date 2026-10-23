package com.unilus.careconnect.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Account not found.");
        }

        String normalizedIdentifier = identifier.trim();

        List<DatabaseUser> users = jdbcTemplate.query("""
                SELECT
                    u.id,
                    u.email,
                    u.password_hash,
                    u.enabled,
                    u.email_verified
                FROM credentials.users u
                LEFT JOIN credentials.student_registration_details s
                    ON s.user_id = u.id
                WHERE lower(u.email) = lower(?)
                   OR lower(s.student_number) = lower(?)
                LIMIT 1
                """,
                (rs, rowNum) -> new DatabaseUser(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("enabled"),
                        rs.getBoolean("email_verified")
                ),
                normalizedIdentifier,
                normalizedIdentifier
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException("Account not found.");
        }

        DatabaseUser databaseUser = users.get(0);

        List<SimpleGrantedAuthority> authorities = jdbcTemplate.query("""
                SELECT r.name
                FROM credentials.roles r
                JOIN credentials.user_roles ur
                    ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.name
                """,
                (rs, rowNum) -> new SimpleGrantedAuthority(
                        "ROLE_" + rs.getString("name")
                ),
                databaseUser.id()
        );

        return User.withUsername(databaseUser.email())
                .password(databaseUser.passwordHash())
                .authorities(authorities)
                .disabled(
                        !databaseUser.enabled()
                                || !databaseUser.emailVerified()
                )
                .build();
    }

    private record DatabaseUser(
            java.util.UUID id,
            String email,
            String passwordHash,
            boolean enabled,
            boolean emailVerified) {
    }
}