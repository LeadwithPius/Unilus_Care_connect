package com.unilus.careconnect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.unilus.careconnect.service.AdminBootstrapService;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService adminBootstrapService;
    private final boolean bootstrapEnabled;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(
            AdminBootstrapService adminBootstrapService,
            @Value("${careconnect.admin.bootstrap-enabled:false}")
            boolean bootstrapEnabled,
            @Value("${careconnect.admin.email:}")
            String adminEmail,
            @Value("${careconnect.admin.password:}")
            String adminPassword) {

        this.adminBootstrapService = adminBootstrapService;
        this.bootstrapEnabled = bootstrapEnabled;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (!bootstrapEnabled) {
            LOGGER.debug(
                    "Administrator bootstrap is disabled."
            );

            return;
        }

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "Administrator bootstrap is enabled, but "
                            + "CARECONNECT_ADMIN_EMAIL is not configured."
            );
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Administrator bootstrap is enabled, but "
                            + "CARECONNECT_ADMIN_PASSWORD is not configured."
            );
        }

        adminBootstrapService.bootstrapAdmin(
                adminEmail,
                adminPassword
        );
    }
}