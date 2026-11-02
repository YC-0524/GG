package com.group17.game.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.HashUtil;

import jakarta.validation.ConstraintViolationException;

// Admin and moderator accounts for the deployed site. Credentials come from the
// ADMIN_* and MODERATOR_* environment variables (set them in the Render dashboard),
// so no password is stored in the repo. An account is skipped if its variables are blank
// or its email already belongs to a different account.
@Component
@Profile("prod")
public class StaffAccountSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StaffAccountSeeder.class);

    private final UserRepository userRepository;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${MODERATOR_EMAIL:}")
    private String moderatorEmail;

    @Value("${MODERATOR_PASSWORD:}")
    private String moderatorPassword;

    public StaffAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        seed("Admin", adminEmail, adminPassword, UserRole.ADMIN);
        seed("Moderator", moderatorEmail, moderatorPassword, UserRole.MODERATOR);
    }

    private void seed(String username, String email, String password, UserRole role) {
        // Values pasted into the Render dashboard can pick up stray whitespace, and login trims the email
        email = email.trim();
        password = password.trim();

        if (email.isBlank() || password.isBlank()) {
            log.info("No {} credentials set, skipping {} account", role, username);
            return;
        }
        if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
            log.warn("{} password is shorter than {} characters, skipping {} account",
                    role, User.MINIMUM_PASSWORD_LENGTH, username);
            return;
        }

        // Never promote an existing account: anyone can register the email on the public site first,
        // and they would become staff with their own password
        User existing = userRepository.findByEmail(email);
        if (existing != null) {
            if (existing.getRole() != role) {
                log.warn("{} is already registered as {}, not creating the {} account. Pick another email",
                        email, existing.getRole(), role);
            }
            return;
        }

        User user = new User(username, email, HashUtil.sha256Hash(password));
        user.setRole(role);
        try {
            userRepository.save(user);
        } catch (ConstraintViolationException ex) {
            // A typo in the email shouldn't stop the whole site from starting
            log.error("Could not create the {} account, check {}_EMAIL: {}", role, role, ex.getMessage());
        }
    }
}
