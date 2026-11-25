package com.group17.game.Controllers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Models.UserReport;
import com.group17.game.Models.UserReportRepository;
import com.group17.game.Models.UserRepository;
import com.group17.game.Models.UserRole;

@Controller
@RequestMapping("/report")
public class ReportController {
    private final UserRepository userRepository;
    private final UserReportRepository reportRepository;

    public ReportController(UserRepository userRepository, UserReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
    }

    boolean isValidReason(String reason) {
        return reason.equals("harassment") ||
                reason.equals("cheating") ||
                reason.equals("offensive") ||
                reason.equals("other");
    }

    @PostMapping("/user")
    public String viewTagsPage(
            Model model,
            @RequestParam Long reportedUserID,
            @RequestParam String description,
            @RequestParam String reason,
            @CurrentUser User user) {
        if (user.getId() == reportedUserID) {
            return "redirect:/dashboard?error=You can't report yourself";
        }

        if (!isValidReason(reason)) {
            return "redirect:/dashboard?error=Invalid report reason";
        }

        if (description.length() < 5) {
            return "redirect:/dashboard?error=Report description must be at least 5 characters";
        }

        Optional<User> reportedUser = userRepository.findById(reportedUserID);
        if (reportedUser.isEmpty()) {
            return "redirect:/dashboard?error=Must report an actual user";
        }

        UserReport newReport = new UserReport(
                user.getId(),
                reportedUser.get().getId(),
                reason,
                description);

        reportRepository.save(newReport);

        return "redirect:/dashboard";
    }

    @GetMapping("/review")
    public String moderatorReportView(Authentication authentication,
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

        List<UserReport> unresolvedReports = reportRepository.findByResolvedAtIsNullOrderByCreatedAtDesc();
        model.addAttribute("reports", unresolvedReports);

        return "reports";
    }

    @PostMapping("/resolve/{reporter}/{reported}")
    public String resolveReport(
            Authentication authentication,
            Model model,
            @CurrentUser User user,
            @PathVariable("reporter") Long reporterID,
            @PathVariable("reported") Long reportedID) {
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

        Optional<UserReport> reportOption = reportRepository.findByUserIDAndReportedUserID(reporterID, reportedID);
        if (reportOption.isEmpty()) {
            return "redirect:/report/review?error=Invalid report combination";
        }

        UserReport report = reportOption.get();
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(user.getId());

        reportRepository.save(report);

        return "redirect:/report/review";
    }
}
