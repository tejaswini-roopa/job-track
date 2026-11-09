package com.jobtrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.jobtrack.entity.User;
import com.jobtrack.entity.enums.Role;
import com.jobtrack.repository.UserRepository;

/**
 * Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD so no credentials live in the code.
 * Does nothing when the variables are not set or the admin already exists.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email:}") String email,
                            @Value("${app.admin.password:}") String password,
                            @Value("${app.admin.full-name:Placement Admin}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set: skipping admin creation");
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD must be at least 8 characters: skipping admin creation");
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(existing -> {
            // Repairs the placeholder admin row that Phase 6's seed script used to insert.
            if (existing.getRole() == Role.ADMIN && !existing.getPasswordHash().startsWith("$2")) {
                existing.setPasswordHash(passwordEncoder.encode(password));
                userRepository.save(existing);
                log.info("Replaced placeholder password for admin {}", normalizedEmail);
            }
        }, () -> {
            User admin = new User();
            admin.setEmail(normalizedEmail);
            admin.setFullName(fullName);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Created admin account {}", normalizedEmail);
        });
    }
}
