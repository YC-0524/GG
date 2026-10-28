package com.group17.game.Controllers;

import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import com.group17.game.DTOs.DiscordTokenResponse;
import com.group17.game.DTOs.DiscordUserResponse;
import com.group17.game.Models.LoginConnection;
import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.LoginMethod;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import jakarta.validation.ConstraintViolationException;

@Controller
@RequestMapping("/auth") // just appends /auth to other routes in here
public class AuthController {
    private final UserRepository userRepository;
    private final LoginConnectionRepository loginConnectionRepository;

    // https://stackoverflow.com/questions/30528255/how-to-access-a-value-defined-in-the-application-properties-file-in-spring-boot
    @Value("${discord.oauth.client_id}")
    private String DISCORD_CLIENT_ID;
    @Value("${discord.oauth.client_secret}")
    private String DISCORD_CLIENT_SECRET;
    @Value("${discord.oauth.redirect_uri}")
    private String DISCORD_REDIRECT_URI;
    @Value("${discord.oauth.oauth_uri}")
    private String DISCORD_OAUTH_URI;

    public AuthController(UserRepository userRepo, LoginConnectionRepository connectionRepository) {
        this.userRepository = userRepo;
        this.loginConnectionRepository = connectionRepository;
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

    @GetMapping("/discord")
    public String redirectToDiscord() {
        return "redirect:" + DISCORD_OAUTH_URI;
    }

    @GetMapping("/discord/callback")
    public String loginWithDiscord(@RequestParam("code") String code) {
        RestClient restClient = RestClient.create();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", DISCORD_CLIENT_ID);
        form.add("client_secret", DISCORD_CLIENT_SECRET);
        form.add("redirect_uri", DISCORD_REDIRECT_URI);

        DiscordTokenResponse tokenResponse = restClient.post()
                .uri("https://discord.com/api/v10/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(DiscordTokenResponse.class);

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            return "redirect:/login.html?error=Discord token failed";
        }

        // 2. Use that token to get the User's info
        DiscordUserResponse discordUser = restClient.get()
                .uri("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .retrieve()
                .body(DiscordUserResponse.class);

        if (discordUser.getEmail() == null || discordUser.getEmail().length() <= 0) {
            return "redirect:/login.html?error=Discord email missing";
        }

        User user = userRepository.findByEmail(discordUser.getEmail());
        if (user == null) {
            // create new user

            // we need to generate a temporary password that the user can then set
            // TODO: make this something like "adjective noun 123" and then store a
            // temporary "PassowrdLoginFlow" entity
            Random random = new Random();
            Long tempPassword = random.nextLong();
            try {
                user = new User(discordUser.getUsername(), discordUser.getEmail(),
                        HashUtil.sha256Hash(tempPassword.toString()));
                // https://docs.discord.com/developers/reference#image-formatting
                user.setAvatar(
                        "https://cdn.discordapp.com/avatars/" + discordUser.getId() + "/" + discordUser.getAvatar());
                userRepository.save(user);
                // TODO: Remove
                System.out.println("Temp password for " + user.getId() + ": " + tempPassword);
            } catch (ConstraintViolationException ex) {
                return "redirect:/login.html?error=Failed to create user";
            }
        }

        String loginID = "discord-" + discordUser.getId();
        LoginConnection connection = loginConnectionRepository.findByLoginID(loginID);
        if (connection == null) {
            try {
                connection = new LoginConnection(loginID, user.getId(), user.getEmail(), LoginMethod.DISCORD,
                        new Date());
                loginConnectionRepository.save(connection);
            } catch (ConstraintViolationException ex) {
                return "redirect:/login.html?error=Failed to create connection";
            }
        }

        return "redirect:/profile/" + user.getId();
    }

    @PostMapping("/login-email")
    public String registerNewAccountViaEmail(
            @RequestParam String email,
            @RequestParam String password) {
        if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
            return "redirect:/login.html?error=Invalid password";
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return "redirect:/login.html?error=Account not found";
        }

        String hash = HashUtil.sha256Hash(password);
        if (!hash.equals(user.getPassword_hash())) {
            return "redirect:/login.html?error=Invalid credentials";
        }

        return "redirect:/profile/" + user.getId();
    }
}
