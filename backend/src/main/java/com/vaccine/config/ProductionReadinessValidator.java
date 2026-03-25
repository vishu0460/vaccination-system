package com.vaccine.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionReadinessValidator {

    private static final List<String> FORBIDDEN_JWT_SECRETS = List.of(
        "",
        "change_this_to_strong_secret_key_min_32_chars",
        "vaxzone-secret-key-for-jwt-token-generation-2024",
        "your-super-secret-jwt-key-at-least-32-bytes-base64-encoded"
    );

    private static final List<String> FORBIDDEN_ADMIN_PASSWORDS = List.of(
        "",
        "Vaccine@#6030",
        "ChangeThisImmediately!",
        "Admin@123"
    );

    private final Environment environment;

    @Value("${security.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @PostConstruct
    void validate() {
        if (!Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase)) {
            return;
        }

        if (FORBIDDEN_JWT_SECRETS.contains(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException("Production startup blocked: configure a strong JWT_SECRET with at least 32 characters.");
        }

        if (FORBIDDEN_ADMIN_PASSWORDS.contains(adminPassword) || adminPassword.length() < 12) {
            throw new IllegalStateException("Production startup blocked: configure a strong DEFAULT_ADMIN_PASSWORD with at least 12 characters.");
        }

        if (seedEnabled) {
            throw new IllegalStateException("Production startup blocked: APP_SEED_ENABLED must be false.");
        }

        String normalizedOrigins = corsAllowedOrigins == null ? "" : corsAllowedOrigins.toLowerCase();
        if (normalizedOrigins.contains("localhost") || normalizedOrigins.contains("127.0.0.1")) {
            throw new IllegalStateException("Production startup blocked: CORS_ALLOWED_ORIGINS must not include localhost entries.");
        }
    }
}
