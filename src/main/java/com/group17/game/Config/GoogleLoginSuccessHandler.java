
package com.group17.game.Config;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.group17.game.Models.LoginConnection;
import com.group17.game.Models.LoginConnectionRepository;
import com.group17.game.Models.LoginMethod;
import com.group17.game.Models.User;
import com.group17.game.Models.UserRepository;
import com.group17.game.Utilities.HashUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final LoginConnectionRepository loginConnectionRepository;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public GoogleLoginSuccessHandler(
            UserRepository userRepository,
            LoginConnectionRepository loginConnectionRepository) {

        this.userRepository = userRepository;
        this.loginConnectionRepository = loginConnectionRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();

        // Get Google's unique account ID and email.
        String googleId = googleUser.getAttribute("sub");
        String email = googleUser.getAttribute("email");

        Boolean emailVerified = googleUser.getAttribute("email_verified");

        String name = googleUser.getAttribute("name");
        String avatar = googleUser.getAttribute("picture");

        // Only accept verified Google email addresses.
        if (googleId == null || email == null ||
                !Boolean.TRUE.equals(emailVerified)) {
            response.sendRedirect("/auth/login?error=Google account information invalid");
            return;
        }

        email = email.trim().toLowerCase();

        // Google-specific login ID.
        String loginID = "google-" + googleId;

        // Check whether this Google account already exists.
        LoginConnection connection =
                loginConnectionRepository.findByLoginID(loginID);

        User user;

        if (connection != null) {

            // Existing Google account: retrieve its GG user.
            user = userRepository.findById(connection.getUserID()).orElse(null);

            if (user == null) {
                response.sendRedirect("/auth/login?error=Linked account not found");
                return;
            }

        } else {

            // Check whether the email already belongs to a GG user.
            user = userRepository.findByEmail(email);

            if (user == null) {

                // Create a unique username.
                String username = name;

                if (username == null || username.isBlank()) {
                    username = email.substring(0, email.indexOf("@"));
                }

                String baseUsername = username;
                int suffix = 1;

                while (userRepository.findByUsername(username) != null) {
                    username = baseUsername + suffix;
                    suffix++;
                }

                // Generate a random temporary password.
                String temporaryPassword = UUID.randomUUID().toString();

                String passwordHash =
                        HashUtil.sha256Hash(temporaryPassword);

                // Create the new GG user.
                user = new User(
                        username,
                        email,
                        passwordHash
                );

                if (avatar != null && !avatar.isBlank()) {
                    user.setAvatar(avatar);
                }

                userRepository.save(user);
            }

            // Create the Google login connection.
            connection = new LoginConnection(
                    loginID,
                    user.getId(),
                    email,
                    LoginMethod.GOOGLE,
                    new Date()
            );

            loginConnectionRepository.save(connection);
        }

        // Sign the user into GG using their numeric GG user ID.
        Authentication ggAuthentication =
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        user.getId(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole()
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(ggAuthentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        // Redirect to the user's GG profile.
        response.sendRedirect("/profile/" + user.getId());
    }
}
