package com.group17.game.Controllers;

import java.util.List;

import com.group17.game.DTOs.UserSummary;
import com.group17.game.Models.RequestStatus;
import com.group17.game.Models.SuspensionRequestRepository;
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
    private final SuspensionRequestRepository requestRepository;

    public DashboardController(
            UserRepository userRepository,
            SuspensionRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
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
    public String showModeratorTools(org.springframework.security.core.Authentication authentication, Model model) {
        // ensures user is logged in
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof Long)) {
                return "redirect:/auth/login";
        }

        // gets the user, checks if they are admin
        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null || (currentUser.getRole() != UserRole.MODERATOR && currentUser.getRole() != UserRole.ADMIN)) {
            return "redirect:/dashboard";
        }
        
        List<UserSummary> users = userRepository.findAll().stream().map(this::toSummary).toList();

        model.addAttribute("users", users);
        return "moderator";
    }

    @GetMapping("/admin")
    public String showAdminTools(org.springframework.security.core.Authentication authentication, Model model) {
        // ensures user is logged in
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof Long)) {
                return "redirect:/auth/login";
        }

        // gets the user, checks if they are admin
        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/dashboard";
        }
        
        List<UserSummary> users = userRepository.findAll().stream().map(this::toSummary).toList();

        model.addAttribute("users", users);
        model.addAttribute("requests", requestRepository.findByStatus(RequestStatus.PENDING));

        return "admin";
    }

    // grabs user info
    private UserSummary toSummary(User user) {
        return new UserSummary (
            user.getId(),
            user.getUsername(),
            user.getAvatar(),
            user.getEmail(),
            user.getRole(),
            user.getStatus(),
            user.getRegistered()
        );
    }
}
