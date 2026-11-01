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
import com.group17.game.Models.ChangePasswordFlow;
import com.group17.game.Models.ChangePasswordFlowRepository;
import com.group17.game.Models.LoginConnection;
import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.LoginMethod;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final LoginConnectionRepository loginConnectionRepository;
    private final ChangePasswordFlowRepository changePasswordFlowRepository;

    // Store the authentication context in the user's HTTP session.
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

    // Inject the repositories needed for authentication and password resets.
    public AuthController(
            UserRepository userRepository,
            LoginConnectionRepository loginConnectionRepository,
            ChangePasswordFlowRepository changePasswordFlowRepository) {

        this.userRepository = userRepository;
        this.loginConnectionRepository = loginConnectionRepository;
        this.changePasswordFlowRepository = changePasswordFlowRepository;
    }

    // Authenticate a user and save their login information in the session.
    public void buildPersistence(
            User user,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, request, response);
    }

    // Create a password-reset flow for a user who needs to set a password.
    public void forceResetPasswordFlow(User user) {
        ChangePasswordFlow newFlow = new ChangePasswordFlow(
                user.getId(),
                true,
                HashUtil.sha256Hash(HashUtil.generateRandomHash())
        );

        changePasswordFlowRepository.save(newFlow);
    }

    // Display the login page.
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/login.html";
    }

    // Display the registration page.
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/register.html";
    }

    // Display the password-reset page.
    @GetMapping("/set-password")
    public String passwordFlowPage() {
        return "passwordFlow";
    }

    // Update a user's password using a valid password-reset flow.
    @PostMapping("/set-password")
    public String updatePasswordFromFlow(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @RequestParam String secretKey) {

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/auth/set-password?error=Passwords must be the same";
        }

        ChangePasswordFlow flow =
                changePasswordFlowRepository.findBySecretKey(secretKey);

        if (flow == null || !flow.isActive()) {
            return "redirect:/";
        }

        // Find the user associated with the password-reset flow.
        User user = userRepository.findById(flow.getUserID()).orElse(null);

        if (user == null) {
            return "redirect:/";
        }

        // Save the new password and deactivate the reset flow.
        user.setPasswordHash(HashUtil.sha256Hash(newPassword));
        flow.setActive(false);

        userRepository.save(user);
        changePasswordFlowRepository.save(flow);

        return "redirect:/dashboard";
    }

    // Register a new account using an email address and password.
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
            // Hash the password before saving the new user.
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

    // Redirect the user to Discord's OAuth login page.
    @GetMapping("/discord")
    public String redirectToDiscord() {
        return "redirect:" + discordOauthUri;
    }

    // Handle the callback after Discord authentication.
    @GetMapping("/discord/callback")
    public String loginWithDiscord(
            @Nullable @RequestParam("code") String code,
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable @RequestParam("error") String error) {

        // Handle a cancelled or unsuccessful Discord login.
        if (error != null || code == null) {
            return "redirect:/auth/login?error=Cancelled login";
        }

        try {
            RestClient restClient = RestClient.create();

            // Prepare the authorization-code exchange request.
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
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

            // Retrieve the authenticated Discord user's information.
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
            String loginID = "discord-" + discordUser.getId();

            // Check whether this Discord account is already linked.
            LoginConnection connection =
                    loginConnectionRepository.findByLoginID(loginID);

            User user;

            if (connection != null) {
                user = userRepository.findById(connection.getUserID()).orElse(null);

                if (user == null) {
                    return "redirect:/auth/login?error=Linked account not found";
                }
            } else {
                // Reuse an existing account with the same email if available.
                user = userRepository.findByEmail(email);

                if (user == null) {
                    // Create a new user with a random temporary password.
                    String temporaryPassword = UUID.randomUUID().toString();

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

                    // Require the newly created Discord user to set a password.
                    forceResetPasswordFlow(user);
                }

                // Link the Discord account to the application user.
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

    // Authenticate an existing user using their email and password.
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

        // Compare the submitted password hash with the stored password hash.
        String hash = HashUtil.sha256Hash(password);

        if (!hash.equals(user.getPasswordHash())) {
            return "redirect:/login.html?error=Invalid+credentials";
        }

        buildPersistence(user, request, response);

        return "redirect:/profile/" + user.getId();
    }
}