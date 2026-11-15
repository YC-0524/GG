package com.group17.game.Controllers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.group17.game.Models.ChangePasswordFlowRepository;
import com.group17.game.Models.RecoveryAttempt;
import com.group17.game.Models.RecoveryAttemptRepository;
import com.group17.game.Models.SecurityAnswer;
import com.group17.game.Models.SecurityAnswerRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;
import com.group17.game.Utilities.PasswordPolicy;
import com.group17.game.Utilities.SecurityQuestions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/auth")
public class AccountRecoveryController {
    private static final String RECOVERY_USER = "recoverUserId";
    private static final String RESET_USER = "resetUserId";
    private static final String RESET_EXPIRES = "resetExpiresAt";

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(60);
    private static final long RESET_WINDOW_MS = 600 * 1000; // 10 min

    private final UserRepository userRepository;
    private final SecurityAnswerRepository securityAnswerRepository;
    private final ChangePasswordFlowRepository changePasswordFlowRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;

    public AccountRecoveryController(
        UserRepository userRepository,
        SecurityAnswerRepository securityAnswerRepository,
        ChangePasswordFlowRepository changePasswordFlowRepository,
        RecoveryAttemptRepository recoveryAttemptRepository) {
            this.userRepository = userRepository;
            this.changePasswordFlowRepository = changePasswordFlowRepository;
            this.securityAnswerRepository = securityAnswerRepository;
            this.recoveryAttemptRepository = recoveryAttemptRepository;
    }

    // find by email
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("mode", "forgot-email");
        return "login";
    }

    @PostMapping("/forgot-password")
    public String startRecovery(@RequestParam String email, HttpSession session) {
        clearRecovery(session);
        clearReset(session);

        User user = userRepository.findByEmail(email.trim());
        if (user == null || !securityAnswerRepository.existsByUserID(user.getId())) {
            return "redirect:/auth/forgot-password?error=" + encode("We couldn't find an account with security questions for that email.");
        }

        Instant lockedUntil = lockedUntil(user.getId());
        if (lockedUntil != null) {
            return "redirect:/auth/forgot-password?error=" + encode(lockedMessage(lockedUntil));
        }
        
        session.setAttribute(RECOVERY_USER, user.getId());
        return "redirect:/auth/forgot-password/questions";
    }
    
    // answer questions
    @GetMapping("/forgot-password/questions")
    public String recoveryQuestionsPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute(RECOVERY_USER);

        if (userId == null) return "redirect:/auth/forgot-password";

        Instant lockedUntil = lockedUntil(userId);
        if (lockedUntil != null) {
            clearRecovery(session);
            return "redirect:/auth/forgot-password?error=" + encode(lockedMessage(lockedUntil));
        }

        List<SecurityAnswer> saved = securityAnswerRepository.findByUserIDOrderByIdAsc(userId);
        if (saved.isEmpty()) {
            clearRecovery(session);
            return "redirect:/auth/forgot-password";
        }

        model.addAttribute("mode", "forgot-questions");
        model.addAttribute("recoveryQuestions", saved.stream().map(SecurityAnswer::getQuestion).toList());

        return "login";
    }

    @PostMapping("/forgot-password/questions")
    public String checkRecoveryAnswers(HttpServletRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute(RECOVERY_USER);

        if (userId == null) return "redirect:/auth/forgot-password";

        Instant lockedUntil = lockedUntil(userId);
        if (lockedUntil != null) {
            clearRecovery(session);
            return "redirect:/auth/forgot-password?error=" + encode(lockedMessage(lockedUntil));
        }

        List<SecurityAnswer> saved = securityAnswerRepository.findByUserIDOrderByIdAsc(userId);
        List<String> answers = SecurityQuestions.formValues(request, "answers");

        boolean allCorrect = !saved.isEmpty() && answers.size() == saved.size();
        for (int i = 0; allCorrect && i < saved.size(); i++) {
            allCorrect = SecurityQuestions.matches(userId, answers.get(i), saved.get(i).getAnswerHash());
        }

        if (!allCorrect) {
            int remaining = recordFailure(userId);

            if (remaining == 0) {
                clearRecovery(session);
                return "redirect:/auth/forgot-password?error=" 
                        + encode(lockedMessage(Instant.now().plus(LOCK_DURATION)));
            }

            return "redirect:/auth/forgot-password/questions?error=" 
                        + encode("One or more answers is incorrect. " + remaining + (remaining == 1 ? " attempt" : " attempts") + " left.");
        }

        clearFailures(userId);
        clearRecovery(session);
        session.setAttribute(RESET_USER, userId);
        session.setAttribute(RESET_EXPIRES, System.currentTimeMillis() + RESET_WINDOW_MS);
        return "redirect:/auth/reset-password";
    }


    // choose new password
    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session, Model model) {
        if (getResetUserId(session) == null) {
            return "redirect:/auth/forgot-password?error="
                    + encode("Your reset session has expired. Please start again.");
        }

        model.addAttribute("mode", "reset");
        return "login";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session) {

        Long userId = getResetUserId(session);
        if (userId == null) {
            return "redirect:/auth/forgot-password?error="
                    + encode("Your reset session has expired. Please start again.");
        }

        Optional<String> problem = PasswordPolicy.validate(newPassword, confirmPassword);
        if (problem.isPresent()) {
            return "redirect:/auth/reset-password?error=" + encode(problem.get());
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            clearReset(session);
            return "redirect:/auth/forgot-password";
        }

        user.setPasswordHash(HashUtil.sha256Hash(newPassword));
        userRepository.save(user);

        // if they still had a forced "set your password" step (e.g. Discord sign-up), it's done now
        changePasswordFlowRepository.findByUserIDAndActiveTrue(userId).ifPresent(flow -> {
            flow.setActive(false);
            changePasswordFlowRepository.save(flow);
        });

        clearReset(session);
        return "redirect:/auth/login?reset";
    }


    // helpers
    private static String encode(String text) {
        return UriUtils.encodeQueryParam(text, StandardCharsets.UTF_8);
    }

    private Instant lockedUntil(long userId) {
        return recoveryAttemptRepository.findByUserID(userId).filter(attempt -> attempt.isLocked(Instant.now()))
                .map(RecoveryAttempt::getLockedUntil).orElse(null);
    }

    private static String lockedMessage(Instant lockedUntil) {
        long seconds = Duration.between(Instant.now(), lockedUntil).getSeconds();
        long minutes = Math.max(1, (seconds + 59) / 60); // round up
        return "Too many incorrect attempts. Please try again in " + minutes
                + (minutes == 1 ? " minute." : " minutes.");
    }

    // saves one failed attempt and returns how many are left (0 means it's now locked)
    private int recordFailure(long userId) {
        Instant now = Instant.now();
        RecoveryAttempt attempt = recoveryAttemptRepository.findByUserID(userId)
                .orElseGet(() -> new RecoveryAttempt(userId));

        // old failures (from before the last lock period) don't count anymore
        if (attempt.getLastFailedAt() != null && attempt.getLastFailedAt().isBefore(now.minus(LOCK_DURATION))) {
            attempt.setFailedAttempts(0);
        }

        attempt.setFailedAttempts(attempt.getFailedAttempts() + 1);
        attempt.setLastFailedAt(now);

        int remaining = MAX_ATTEMPTS - attempt.getFailedAttempts();
        if (remaining <= 0) {
            attempt.setLockedUntil(now.plus(LOCK_DURATION));
            attempt.setFailedAttempts(0); // fresh tries once the lock ends
            remaining = 0;
        }

        recoveryAttemptRepository.save(attempt);
        return remaining;
    }

    // correct answers: forget earlier failures
    private void clearFailures(long userId) {
        recoveryAttemptRepository.findByUserID(userId).ifPresent(recoveryAttemptRepository::delete);
    }

    private Long getResetUserId(HttpSession session) {
        Object userId = session.getAttribute(RESET_USER);
        Object expires = session.getAttribute(RESET_EXPIRES);

        if (!(userId instanceof Long id) || !(expires instanceof Long expiresAt)) {
            return null;
        }
        if (System.currentTimeMillis() > expiresAt) {
            clearReset(session);
            return null;
        }
        return id;
    }

    private static void clearRecovery(HttpSession session) {
        session.removeAttribute(RECOVERY_USER);
    }

    private static void clearReset(HttpSession session) {
        session.removeAttribute(RESET_USER);
        session.removeAttribute(RESET_EXPIRES);
    }
}
