package com.group17.game.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import jakarta.validation.ConstraintViolationException;

@Controller
@RequestMapping("/auth") // just appends /auth to other routes in here
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepo) {
        this.userRepository = userRepo;
    }

    @PostMapping("/register-email")
    public String registerNewAccountViaEmail(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password) {
        try {
            if (userRepository.findByEmail(email) != null) {
                return "redirect:/register.html?error=Email already registered";
            }

            if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
                return "redirect:/register.html?error=Password too short";
            }

            // converts plain text into something more secure
            String passwordHash = HashUtil.sha256Hash(password);
            User newUser = new User(username, email, passwordHash);

            userRepository.save(newUser);
            return "redirect:/profile/" + newUser.getId();
        } catch (ConstraintViolationException ex) {
            // maybe make errors a little more clear
            System.out.println("Register account exception: " + ex);
            return "redirect:/register.html?error=Invalid user details";
        }
    }

    @PostMapping("/login-email")
    public String registerNewAccountViaEmail(
            @RequestParam String email,
            @RequestParam String password) {
        if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
            return "redirect:/login.html?error=Invalid password";
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            return "redirect:/login.html?error=No matching Email";
        }

        String hash = HashUtil.sha256Hash(password);
        if (!hash.equals(user.getPassword_hash())) {
            return "redirect:/login.html?error=Invalid credentials";
        }

        return "redirect:/profile/" + user.getId();
    }
}
