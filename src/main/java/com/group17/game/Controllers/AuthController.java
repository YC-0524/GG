
package com.group17.game.Controllers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final LoginConnectionRepository loginConnectionRepository;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Value("${discord.oauth.client_id}")
    private String discordClientId;

    @Value("${discord.oauth.client_secret}")
    private String discordClientSecret;

    @Value("${discord.oauth.redirect_uri}")
    private String discordRedirectUri;

    @Value("${discord.oauth.oauth_uri}")
    private String discordOauthUri;

    public AuthController(
            UserRepository userRepository,
            LoginConnectionRepository loginConnectionRepository) {

        this.userRepository = userRepository;
        this.loginConnectionRepository = loginConnectionRepository;
    }

    private void buildPersistence(
            User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole())));

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context, request, response);
    }

    // LOGIN PAGE
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/login.html";
    }

    // REGISTRATION PAGE
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/register.html";
    }

    // EMAIL REGISTRATION
    @PostMapping("/register-email")
    public String registerNewAccountViaEmail(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        email = email.trim().toLowerCase();

        if (userRepository.findByEmail(email) != null) {
            return "redirect:/register.html?error=Email+already+registered";
        }

        if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
            return "redirect:/register.html?error=Password+too+short";
        }

        try {
            String passwordHash = HashUtil.sha256Hash(password);

            User newUser = new User(
                    username.trim(),
                    email,
                    passwordHash
            );

            userRepository.save(newUser);

            buildPersistence(newUser, request, response);

            return "redirect:/profile/" + newUser.getId();

        } catch (ConstraintViolationException ex) {
            return "redirect:/register.html?error=Invalid+user+details";
        }
    }

    // DISCORD REDIRECT
    @GetMapping("/discord")
    public String redirectToDiscord() {
        return "redirect:" + discordOauthUri;
    }

    // DISCORD CALLBACK
    @GetMapping("/discord/callback")
    public String loginWithDiscord(
            @RequestParam("code") String code,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            RestClient restClient = RestClient.create();

            MultiValueMap<String, String> form =
                    new LinkedMultiValueMap<>();

            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("client_id", discordClientId);
            form.add("client_secret", discordClientSecret);
            form.add("redirect_uri", discordRedirectUri);

            DiscordTokenResponse tokenResponse =
                    restClient.post()
                            .uri("https://discord.com/api/v10/oauth2/token")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(DiscordTokenResponse.class);

            if (tokenResponse == null ||
                    tokenResponse.getAccessToken() == null) {
                return "redirect:/login.html?error=Discord+authentication+failed";
            }

            DiscordUserResponse discordUser =
                    restClient.get()
                            .uri("https://discord.com/api/users/@me")
                            .header(
                                    "Authorization",
                                    "Bearer " + tokenResponse.getAccessToken()
                            )
                            .retrieve()
                            .body(DiscordUserResponse.class);

            if (discordUser == null ||
                    discordUser.getEmail() == null ||
                    discordUser.getEmail().isBlank()) {
                return "redirect:/login.html?error=Discord+email+missing";
            }

            String email = discordUser.getEmail().trim().toLowerCase();

            User user = userRepository.findByEmail(email);

            if (user == null) {

                String temporaryPassword =
                        UUID.randomUUID().toString();

                user = new User(
                        discordUser.getUsername(),
                        email,
                        HashUtil.sha256Hash(temporaryPassword)
                );

                if (discordUser.getAvatar() != null) {
                    user.setAvatar(
                            "https://cdn.discordapp.com/avatars/"
                            + discordUser.getId()
                            + "/" + discordUser.getAvatar()
                    );
                }

                userRepository.save(user);
            }

            String loginID = "discord-" + discordUser.getId();

            LoginConnection connection =
                    loginConnectionRepository.findByLoginID(loginID);

            if (connection == null) {

                connection = new LoginConnection(
                        loginID,
                        user.getId(),
                        user.getEmail(),
                        LoginMethod.DISCORD,
                        new Date()
                );

                loginConnectionRepository.save(connection);
            }

            buildPersistence(user, request, response);

            return "redirect:/profile/" + user.getId();

        } catch (Exception ex) {
            ex.printStackTrace();

            return "redirect:/login.html?error=Discord+login+failed";
        }
    }

    // EMAIL LOGIN
    @PostMapping("/login-email")
    public String loginWithEmail(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = userRepository.findByEmail(
                email.trim().toLowerCase()
        );

        if (user == null) {
            return "redirect:/login.html?error=Invalid+credentials";
        }

        String hash = HashUtil.sha256Hash(password);

        if (!hash.equals(user.getPasswordHash())) {
            return "redirect:/login.html?error=Invalid+credentials";
        }

        buildPersistence(user, request, response);

        return "redirect:/profile/" + user.getId();
    }
}
