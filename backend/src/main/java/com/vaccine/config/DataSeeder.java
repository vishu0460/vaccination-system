package com.vaccine.config;

import com.vaccine.domain.Role;
import com.vaccine.domain.RoleName;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.RoleRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.admin.email:vaxzone.vaccine@gmail.com}")
    private String adminEmail;

    @Value("${app.admin.password:Vaccine@#6030}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener
    @Transactional
    public void handleContextRefresh(ContextRefreshedEvent event) {
        if (!seedEnabled) {
            log.info("Data seeding disabled");
            return;
        }

        log.info("Starting data seeding...");

        // Create SUPER_ADMIN role if missing
        roleRepository.findByName(RoleName.SUPER_ADMIN)
            .orElseGet(() -> {
                Role superAdminRole = Role.builder()
                    .name(RoleName.SUPER_ADMIN)
                    .build();
                Role saved = roleRepository.save(superAdminRole);
                log.info("Created SUPER_ADMIN role: {}", saved.getId());
                return saved;
            });

        // Create super admin user if missing
        if (!userRepository.existsByEmail(adminEmail.toLowerCase())) {
            Role superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN).get();

            User superAdmin = User.builder()
                .email(adminEmail.toLowerCase())
                .fullName("Super Administrator")
                .password(passwordEncoder.encode(adminPassword))
                .age(0)
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .isSuperAdmin(true)
                .isAdmin(true)
                .roles(Set.of(superAdminRole))
                .build();

            User saved = userRepository.save(superAdmin);
            log.info("Created super admin user: {} (ID: {})", adminEmail, saved.getId());
        } else {
            log.info("Super admin already exists: {}", adminEmail);
        }

        log.info("Data seeding completed");
    }
}

