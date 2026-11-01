package com.group17.game.Controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.CurrentUser;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;

@Controller
@RequestMapping("/profile") // prefixes /profile to other routes in here
public class ProfileController {
    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepo) {
        this.userRepository = userRepo;
    }

    @GetMapping("/me")
    public String getUserProfile(Model model, @CurrentUser User user) {
        return "redirect:/profile/" + user.getId();
    }

    @GetMapping("/search")
    public String searchProfile(
        Model model, 
        @RequestParam (name = "error", required = false) String error,
        @RequestParam (name = "usernameFilter", required = false) String usernameFilter
    ) {
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
    public String getUserProfile(@PathVariable("userID") long userID, Model model) {
        Optional<User> userOption = userRepository.findById(userID);
        if (userOption.isEmpty()) {
            return "redirect:/profile/search?error=User doesn't exist";
        }

        User user = userOption.get();
        user.setPasswordHash("");
        user.setEmail("");
        model.addAttribute("profile", user);
        return "profile";
    }
}
