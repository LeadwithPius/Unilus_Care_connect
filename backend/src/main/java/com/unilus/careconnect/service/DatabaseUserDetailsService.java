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
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        var users = jdbcTemplate.query("""
                SELECT email, password_hash, enabled, email_verified
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """,
                (rs, rowNum) -> new DatabaseUser(
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("enabled"),
                        rs.getBoolean("email_verified")),
                email);

        if (users.isEmpty()) {
            // Avoid including the submitted email in authentication exception messages.
            throw new UsernameNotFoundException("Account not found.");
        }

        DatabaseUser databaseUser = users.get(0);

        List<SimpleGrantedAuthority> authorities = jdbcTemplate.query("""
                SELECT r.name
                FROM credentials.roles r
                JOIN credentials.user_roles ur ON ur.role_id = r.id
                JOIN credentials.users u ON u.id = ur.user_id
                WHERE lower(u.email) = lower(?)
                ORDER BY r.name
                """,
                (rs, rowNum) -> new SimpleGrantedAuthority(
                        "ROLE_" + rs.getString("name")),
                email);

        return User.withUsername(databaseUser.email())
                .password(databaseUser.passwordHash())
                .authorities(authorities)
                .disabled(!databaseUser.enabled() || !databaseUser.emailVerified())
                .build();
    }

    private record DatabaseUser(
            String email,
            String passwordHash,
            boolean enabled,
            boolean emailVerified) {
    }
}
