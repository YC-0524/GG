package com.group17.game.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.group17.game.Models.UserRepository;

@Controller("/auth")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepo) {
        this.userRepository = userRepo;
    }

    @PostMapping("/register-email")
    public String registerNewAccountViaEmail(@RequestParam String email, @RequestParam String password) {
        return "redirect:/profile/" + 0;
    }
}
