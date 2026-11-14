package com.group17.game.Controllers;

import java.util.List;

import com.group17.game.DTOs.UserSummary;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.GameRequestRepository;
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
    private final GameRequestRepository gameRequestRepository;

    public DashboardController(
            UserRepository userRepository,
            SuspensionRequestRepository requestRepository,
            GameRequestRepository gameRequestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.gameRequestRepository = gameRequestRepository;
    }

    @GetMapping("")
    public String showDashboardPage(
            Authentication authentication,
            Model model,
            @CurrentUser User user) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", user);
        model.addAttribute("players",
                userRepository.findPlayerCardsExcluding(userId));

        return "dashboard";
    }

    @GetMapping("/moderator")
    public String showModeratorTools(
            Authentication authentication,
            Model model,
            @CurrentUser User user) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long)) {
            return "redirect:/auth/login";
        }

        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null
                || (currentUser.getRole() != UserRole.MODERATOR
                && currentUser.getRole() != UserRole.ADMIN)) {
            return "redirect:/dashboard";
        }

        List<UserSummary> users = userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("users", users);

        // Pending game requests for moderators to review.
        model.addAttribute("gameRequests",
                gameRequestRepository.findByStatusOrderByCreatedAtDesc(
                        RequestStatus.PENDING));

        return "moderator";
    }

    @GetMapping("/admin")
    public String showAdminTools(
            Authentication authentication,
            Model model,
            @CurrentUser User user) {

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long)) {
            return "redirect:/auth/login";
        }

        Long userId = (Long) authentication.getPrincipal();
        User currentUser = userRepository.findById(userId).orElse(null);

        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/dashboard";
        }

        List<UserSummary> users = userRepository.findAll()
                .stream()
                .map(this::toSummary)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("users", users);
        model.addAttribute("requests",
                requestRepository.findByStatus(RequestStatus.PENDING));

        return "admin";
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(
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
