package com.group17.game.Controllers;

import java.util.Map;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardController {

    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/me")
    @ResponseBody
    public ResponseEntity<?> getCurrentUser(HttpSession session) {

        Object userId = session.getAttribute("userId");

        if (!(userId instanceof Long)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not logged in"));
        }

        User user = userRepository.findById((Long) userId)
                .orElse(null);

        if (user == null || user.getRole() == null) {
            session.invalidate();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid account"));
        }

        return ResponseEntity.ok(
                Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole().name()
                )
        );
    }
}

