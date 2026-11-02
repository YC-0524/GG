package com.group17.game.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import com.group17.game.Utilities.HashUtil;

// Local testing accounts for the staff roles. Only runs with the "dev" profile:
// ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
@Component
@Profile("dev")
public class DevAccountSeeder implements CommandLineRunner {
    public static final String ADMIN_EMAIL = "admin@gg.test";
    public static final String MODERATOR_EMAIL = "moderator@gg.test";
    public static final String DEV_PASSWORD = "gg-dev-password";

    private final UserRepository userRepository;

    public DevAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        createIfMissing("Admin", ADMIN_EMAIL, UserRole.ADMIN);
        createIfMissing("Moderator", MODERATOR_EMAIL, UserRole.MODERATOR);
    }

    private void createIfMissing(String username, String email, UserRole role) {
        if (userRepository.findByEmail(email) != null) {
            return;
        }
        User user = new User(username, email, HashUtil.sha256Hash(DEV_PASSWORD));
        user.setRole(role);
        userRepository.save(user);
    }
}
