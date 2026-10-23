package com.unilus.careconnect;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // Public health check
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/health"
                ).permitAll()

                // Public authentication and account-recovery endpoints
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login",
                    "/api/students/register",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/verify-email"
                ).permitAll()

                // Administrator-only endpoints
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // Counsellor triage endpoints must precede general triage
                // student restrictions.
                .requestMatchers("/api/triage/counsellor/**")
                    .hasRole("COUNSELLOR")

                // Student triage endpoints
                .requestMatchers("/api/triage/**")
                    .hasRole("STUDENT")

                // Counsellor-only endpoints
                .requestMatchers("/api/counsellor/**")
                    .hasRole("COUNSELLOR")

                // Student-only endpoints
                .requestMatchers(
                    "/api/appointments/**",
                    "/api/education/**",
                    "/api/game/**"
                ).hasRole("STUDENT")

                // Other authentication endpoints require authentication
                .requestMatchers("/api/auth/**")
                    .authenticated()

                // All remaining endpoints require authentication
                .anyRequest().authenticated()
            )

            // JWT authentication is used instead of Basic or form login
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter
                    )
                )
            );

        return http.build();
    }
}