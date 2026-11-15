package com.group17.game.Config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// A failed or cancelled Google login goes back to the login page with a message (US 32).
// Without this, Spring sends the player to /login?error, which is an error page.
@Component
public class GoogleLoginFailureHandler implements AuthenticationFailureHandler {
    static final String CANCELLED = "Google login was cancelled. Try again or choose another way to log in.";
    static final String FAILED = "Google login failed. Please try again or choose another way to log in.";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String message = isCancelled(exception) ? CANCELLED : FAILED;
        response.sendRedirect("/auth/login?error=" + UriUtils.encodeQueryParam(message, StandardCharsets.UTF_8));
    }

    // Google sends "access_denied" when the player presses Cancel on its consent screen.
    private boolean isCancelled(AuthenticationException exception) {
        return exception instanceof OAuth2AuthenticationException oauth
                && "access_denied".equals(oauth.getError().getErrorCode());
    }
}
