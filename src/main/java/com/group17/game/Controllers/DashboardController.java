package com.group17.game.Controllers;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("")
    public String showDashboardPage(Authentication authentication, Model model) {
        // The login controller stores the database user ID as the principal.
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            return "redirect:/auth/login";
        }

        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser == null) return "redirect:/auth/login";

        model.addAttribute("currentUsername", currentUser.getUsername());
        model.addAttribute("showModerator", currentUser.getRole() == UserRole.MODERATOR
                || currentUser.getRole() == UserRole.ADMIN);
        model.addAttribute("showAdmin", currentUser.getRole() == UserRole.ADMIN);
        model.addAttribute("players", userRepository.findPlayerCardsExcluding(userId));
        return "dashboard";
    }

    @GetMapping("/moderator")
    public String showModeratorTools() {
        return "moderator";
    }

    @GetMapping("/admin")
    public String showAdminTools() {
        return "admin";
    }
}
