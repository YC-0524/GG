package com.group17.game.Controllers;

import java.util.Date;
import java.util.List;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
import com.group17.game.Models.UserStatus;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

@Controller
@RequestMapping("/auth") // just prefixes /auth to other routes in here
public class AuthController {
    private final UserRepository userRepository;
    private final LoginConnectionRepository loginConnectionRepository;
    private final ChangePasswordFlowRepository changePasswordFlowRepository;
    private SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    // https://stackoverflow.com/questions/30528255/how-to-access-a-value-defined-in-the-application-properties-file-in-spring-boot
    @Value("${discord.oauth.client_id}")
    private String DISCORD_CLIENT_ID;
    @Value("${discord.oauth.client_secret}")
    private String DISCORD_CLIENT_SECRET;
    @Value("${discord.oauth.redirect_uri}")
    private String DISCORD_REDIRECT_URI;
    @Value("${discord.oauth.oauth_uri}")
    private String DISCORD_OAUTH_URI;

    public AuthController(
        UserRepository userRepo, 
        LoginConnectionRepository connectionRepository, 
        ChangePasswordFlowRepository changePasswordFlowRepository
    ) {
        this.userRepository = userRepo;
        this.loginConnectionRepository = connectionRepository;
        this.changePasswordFlowRepository = changePasswordFlowRepository;
    }

    // https://www.javaguides.net/2024/04/spring-security-authentication-manager.html
    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityContext securityContextHolderStrategy() throws Exception {
        return SecurityContextHolder.createEmptyContext();
    }

    public void buildPersistence(User user, HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user.getId(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))); 

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    // returns why login was blocked
    private String getLoginBlockReason(User user) {
        if (user.getStatus() == UserStatus.SUSPENDED) {
            return "Account suspended";
        }

        if (user.getStatus() == UserStatus.TIMED_OUT) {
            Instant until = user.getTimeoutUntil();
            if (until != null && Instant.now().isBefore(until)) {
                return "Account timed out";
            }

            user.setStatus(UserStatus.ACTIVE);
            user.setTimeoutUntil(null);
            userRepository.save(user);
        }

        return null;
    }

    public void forceResetPasswordFlow(User user) {
        ChangePasswordFlow newFlow = new ChangePasswordFlow(
            user.getId(), 
            true, 
            HashUtil.sha256Hash(HashUtil.generateRandomHash())
        );

        changePasswordFlowRepository.save(newFlow);
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/set-password")
    public String passwordFlowPage() {
        return "passwordFlow";
    }

    @PostMapping("/set-password")
    public String updatePasswordFromFlow(
        @RequestParam String newPassword, 
        @RequestParam String confirmPassword,
        @RequestParam String secretKey) {

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/auth/set-password?error=Passwords must be the same";
        }

        ChangePasswordFlow flow = changePasswordFlowRepository.findBySecretKey(secretKey);
        if (flow == null || !flow.isActive()) {
            return "redirect:/";
        }  

        // check if this request is actually a one we can verify
        if (!flow.getSecretKey().equals(secretKey)) {
            return "redirect:/";
        }

        // find the user from the request, not the session
        User user = userRepository.findById(flow.getUserID()).get();
        if (user == null) {
            return "redirect:/";
        }

        // we know that this is the actual user now, so lets set a new password
        user.setPasswordHash(HashUtil.sha256Hash(newPassword));
        flow.setActive(false);
        userRepository.save(user);
        changePasswordFlowRepository.save(flow);

        return "redirect:/dashboard";
    }

    @PostMapping("/register-email")
    public String registerNewAccountViaEmail(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            if (userRepository.findByEmail(email) != null) {
                return "redirect:/auth/register?error=Email already registered";
            }

            if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
                return "redirect:/auth/register?error=Password too short";
            }

            // converts plain text into something more secure
            String passwordHash = HashUtil.sha256Hash(password);
            User newUser = new User(username, email, passwordHash);

            userRepository.save(newUser);

            // Generate token based off password
            buildPersistence(newUser, request, response);

            return "redirect:/dashboard";
        } catch (ConstraintViolationException ex) {
            // maybe make errors a little more clear
            System.out.println("Register account exception: " + ex);
            return "redirect:/auth/register?error=Invalid user details";
        }
    }

    @GetMapping("/discord")
    public String redirectToDiscord() {
        return "redirect:" + DISCORD_OAUTH_URI;
    }

    @GetMapping("/discord/callback")
    public String loginWithDiscord(@Nullable @RequestParam("code") String code, HttpServletRequest request,
            HttpServletResponse response, @Nullable @RequestParam("error") String error) {  
        if (error != null || code == null) {
            return "redirect:/auth/login?error=Cancelled login";
        }

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
            return "redirect:/auth/login?error=Discord token failed";
        }

        // 2. Use that token to get the User's info
        DiscordUserResponse discordUser = restClient.get()
                .uri("https://discord.com/api/users/@me")
                .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                .retrieve()
                .body(DiscordUserResponse.class);

        if (discordUser.getEmail() == null || discordUser.getEmail().length() <= 0) {
            return "redirect:/auth/login?error=Discord email missing";
        }

        User user = userRepository.findByEmail(discordUser.getEmail());
        if (user == null) {
            // create new user

            try {
                user = new User(discordUser.getUsername(), discordUser.getEmail(),
                        HashUtil.sha256Hash(HashUtil.generateRandomHash()));
                // https://docs.discord.com/developers/reference#image-formatting
                user.setAvatar(
                        "https://cdn.discordapp.com/avatars/" + discordUser.getId() + "/" + discordUser.getAvatar());
                userRepository.save(user);

                forceResetPasswordFlow(user);
            } catch (ConstraintViolationException ex) {
                return "redirect:/auth/login?error=Failed to create user";
            }
        }

        String blockReason = getLoginBlockReason(user);
        if (blockReason != null) {
            return "redirect:/login?error=" + blockReason;
        }

        // Generate token based off password
        buildPersistence(user, request, response);

        String loginID = "discord-" + discordUser.getId();
        LoginConnection connection = loginConnectionRepository.findByLoginID(loginID);
        if (connection == null) {
            try {
                connection = new LoginConnection(loginID, user.getId(), user.getEmail(), LoginMethod.DISCORD,
                        new Date());
                loginConnectionRepository.save(connection);
            } catch (ConstraintViolationException ex) {
                return "redirect:/auth/login?error=Failed to create connection";
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/login-email")
    public String registerNewAccountViaEmail(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (password.length() < User.MINIMUM_PASSWORD_LENGTH) {
            return "redirect:/auth/login?error=Invalid password";
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            return "redirect:/auth/login?error=Account not found";
        }

        String hash = HashUtil.sha256Hash(password);
        if (!hash.equals(user.getPasswordHash())) {
            return "redirect:/auth/login?error=Invalid credentials";
        }

        buildPersistence(user, request, response);
        return "redirect:/dashboard";
    }
}
