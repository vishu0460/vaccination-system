package com.vaccine.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductionReadinessValidator {

    private static final List<String> SAFE_TO_SKIP_PROFILES = List.of("test");

    private final Environment environment;

    @PostConstruct
    void validate() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (Arrays.stream(activeProfiles).anyMatch(profile -> SAFE_TO_SKIP_PROFILES.contains(profile.toLowerCase()))) {
            return;
        }

        boolean requiresExplicitDbUrl = Arrays.stream(activeProfiles).anyMatch(profile ->
            "prod".equalsIgnoreCase(profile) || "local-fixed".equalsIgnoreCase(profile));
        if (requiresExplicitDbUrl) {
            requireProperty("DB_URL");
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        if (!datasourceUrl.startsWith("jdbc:h2:")) {
            requireProperty("DB_USERNAME");
            requireProperty("DB_PASSWORD");
        }

        String jwtSecret = requireProperty("JWT_SECRET");
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("Startup blocked: JWT_SECRET must be at least 32 characters.");
        }

        boolean devProfileActive = Arrays.stream(activeProfiles)
            .anyMatch(profile -> "dev".equalsIgnoreCase(profile));
        String seedEnabled = environment.getProperty("APP_SEED_ENABLED");
        if (!devProfileActive && "true".equalsIgnoreCase(seedEnabled)) {
            throw new IllegalStateException("Startup blocked: APP_SEED_ENABLED may only be true for the dev profile.");
        }
    }

    private String requireProperty(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Startup blocked: required configuration value " + key + " is not set.");
        }
        return value;
    }
}
