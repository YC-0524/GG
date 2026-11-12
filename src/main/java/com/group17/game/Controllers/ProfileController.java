package com.group17.game.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.GameEntryRepository;
import com.group17.game.Models.ProfileTag;
import com.group17.game.Models.ProfileTagRepository;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/profile") // prefixes /profile to other routes in here
public class ProfileController {
    private final UserRepository userRepository;
    private final GameEntryRepository gameEntryRepository;
    private final ProfileTagRepository profileTagRepository;

    public ProfileController(UserRepository userRepo, GameEntryRepository gameEntryRepo, ProfileTagRepository profileTagRepository) {
        this.userRepository = userRepo;
        this.gameEntryRepository = gameEntryRepo;
        this.profileTagRepository = profileTagRepository;
    }

    @GetMapping("/me")
    public String getUserProfile(Model model, @CurrentUser User user) {
        return "redirect:/profile/" + user.getId();
    }

    @PostMapping("/me/status")
    public String updateUserActivity(@RequestParam String status, @CurrentUser User user, HttpServletRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        ActivityStatus activityStatus;
        try {
            activityStatus = ActivityStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "/dashboard?error=Failed to update status.";
        }

        user.setActivityStatus(activityStatus);
        userRepository.save(user);

        String referer = request.getHeader("Referer");

        return "redirect:" + (referer != null ? referer : "/profile");
    }

    @GetMapping("/search")
    public String searchProfile(
            Model model,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "usernameFilter", required = false) String usernameFilter) {
        if (usernameFilter != null) {
            List<User> users = userRepository.findByUsernameContaining(usernameFilter);
            for (User user : users) {
                user.setPasswordHash("");
                user.setEmail("");
            }

            model.addAttribute("results", users);
            if (users.size() <= 0) {
                model.addAttribute("error", "No matching users found");
            }
        }

        if (error != null) {
            model.addAttribute("error", error);
        }

        return "profileSearch";
    }

    @GetMapping("/{userID}")
    public String getUserProfile(@PathVariable("userID") long userID, Model model, @Nullable @CurrentUser User user) {
        Optional<User> profileOption = userRepository.findById(userID);
        if (profileOption.isEmpty()) {
            return "redirect:/profile/search?error=User doesn't exist";
        }

        User profile = profileOption.get();
        profile.setPasswordHash("");
        profile.setEmail("");
        model.addAttribute("profile", profile);
        model.addAttribute("user", user);
        model.addAttribute("games", gameEntryRepository.findByUserIdOrderByGameNameAsc(userID));
        model.addAttribute("tags", profileTagRepository.findAllByUserId(user.getId()));
        // Only the owner gets a link to edit their list
        model.addAttribute("isOwner", user != null && user.getId() == userID);
        return "profile";
    }

    @GetMapping ("/{userID}/tags")
    public List<ProfileTag> addTagToUserProfile(@PathVariable("userID") long userID, Model model) { 
        Optional<User> user = userRepository.findById(userID);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<ProfileTag> tags = profileTagRepository.findAllByUserId(userID);
        return tags;
    }
}
