package com.backend.user_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.backend.common.dto.Role;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;

/**
 * Startup utility that ensures a root Administrator account exists in the
 * database.
 * Executes automatically upon application context initialization.
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @SuppressWarnings("java:S2068")
    private static final String DEFAULT_ADMIN_PASSWORD = "CHANGE_ME_IN_PRODUCTION";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:}")
    private String adminSecret;

    @Value("${app.admin.email:admin@admin.com}")
    private String adminEmail;

    @Autowired
    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                log.info("Admin user not found. Initiating creation protocol...");

                String secretValue = (adminSecret == null || adminSecret.isEmpty())
                        ? DEFAULT_ADMIN_PASSWORD
                        : adminSecret;

                if (DEFAULT_ADMIN_PASSWORD.equals(secretValue)) {
                    log.warn("CRITICAL SECURITY WARNING: Admin password is set to default. " +
                            "Please inject 'app.admin.password' via environment variables immediately.");
                }

                User adminUser = User.builder()
                        .firstName("Admin")
                        .lastName("User")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(secretValue))
                        .role(Role.ADMIN)
                        .build();

                userRepository.save(adminUser);
                log.info("System administrator account provisioned successfully.");
            } else {
                log.debug("System administrator account verified.");
            }
        } catch (Exception e) {
            log.error("Failed to execute AdminUserInitializer payload: {}", e.getMessage(), e);
        }
    }
}