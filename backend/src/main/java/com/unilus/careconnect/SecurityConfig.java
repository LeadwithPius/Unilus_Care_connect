package com.unilus.careconnect;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Hashes passwords for student, counsellor, and administrator accounts.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Reads application roles from the "roles" claim in CareConnect JWTs.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    /**
     * Allows the local CareConnect frontend to call the Spring Boot API.
     *
     * Only the explicitly listed local development origins are allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
        ));

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Apply the CORS configuration before authorization checks.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // Allow browser preflight requests without authentication.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public health check.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/health"
                ).permitAll()

                // Public authentication and account-recovery endpoints.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login",
                    "/api/students/register",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/verify-email"
                ).permitAll()

                // Administrator-only endpoints.
                .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                // Counsellor triage endpoints.
                .requestMatchers("/api/triage/counsellor/**")
                    .hasRole("COUNSELLOR")

                // Student triage endpoints.
                .requestMatchers("/api/triage/**")
                    .hasRole("STUDENT")

                // Counsellor-only endpoints.
                .requestMatchers("/api/counsellor/**")
                    .hasRole("COUNSELLOR")

                // Student-only endpoints.
                .requestMatchers(
                    "/api/appointments/**",
                    "/api/education/**",
                    "/api/game/**"
                ).hasRole("STUDENT")

                // Other authentication endpoints require authentication.
                .requestMatchers("/api/auth/**")
                    .authenticated()

                // All remaining endpoints require authentication.
                .anyRequest().authenticated()
            )

            // JWT authentication; no HTTP Basic or form login.
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())

            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            );

        return http.build();
    }
}
