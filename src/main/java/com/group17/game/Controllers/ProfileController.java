package com.group17.game.Controllers;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String getUserProfile(Model model) {
        // TODO: abstract this to a global config
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "redirect:/profile/search?error=Invalid authentication";
        }
        
        long userID = (long)auth.getPrincipal();
        return "redirect:/profile/" + userID;
    }

    @GetMapping("/search")
    public String searchProfile(Model model) {
        // TODO: implement
        return "index";
    }

    @GetMapping("/{userID}")
    public String getUserProfile(@PathVariable("userID") long userID, Model model) {
        Optional<User> userOption = userRepository.findById(userID);
        if (userOption.isEmpty()) {
            return "redirect:/profile/search?error=User doesn't exist";
        }

        User user = userOption.get();
        user.setPasswordHash("");
        model.addAttribute("profile", user);
        return "profile";
    }
}
