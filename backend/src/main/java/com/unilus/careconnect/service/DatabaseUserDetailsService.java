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
                SELECT
                    id,
                    email,
                    password_hash,
                    enabled
                FROM credentials.users
                WHERE lower(email) = lower(?)
                """,
                (rs, rowNum) -> new DatabaseUser(
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getBoolean("enabled")
                ),
                email
        );

        if (users.isEmpty()) {
            throw new UsernameNotFoundException(
                    "No account found for email: " + email
            );
        }

        DatabaseUser databaseUser = users.get(0);

        List<SimpleGrantedAuthority> authorities =
                jdbcTemplate.query("""
                        SELECT r.name
                        FROM credentials.roles r
                        JOIN credentials.user_roles ur
                          ON ur.role_id = r.id
                        JOIN credentials.users u
                          ON u.id = ur.user_id
                        WHERE lower(u.email) = lower(?)
                        ORDER BY r.name
                        """,
                        (rs, rowNum) ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + rs.getString("name")
                                ),
                        email
                );

        return User.withUsername(databaseUser.email())
                .password(databaseUser.passwordHash())
                .authorities(authorities)
                .disabled(!databaseUser.enabled())
                .build();
    }

    private record DatabaseUser(
            String email,
            String passwordHash,
            boolean enabled
    ) {
    }
}