package com.group17.game.Config;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.group17.game.Models.ChangePasswordFlow;
import com.group17.game.Models.ChangePasswordFlowRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PasswordResetFilter extends OncePerRequestFilter {

    private static final String SET_PASSWORD_PATH = "/auth/set-password";

    private final ChangePasswordFlowRepository passwordFlowRepository;

    public PasswordResetFilter(ChangePasswordFlowRepository passwordFlowRepository) {
        this.passwordFlowRepository = passwordFlowRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith(SET_PASSWORD_PATH)
                || path.startsWith("/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {

            Long userId = extractUserId(auth);

            if (userId != null) {
                Optional<ChangePasswordFlow> flow = passwordFlowRepository.findByUserIDAndActiveTrue(userId);

                if (flow.isPresent()) {
                    response.sendRedirect(request.getContextPath()
                            + SET_PASSWORD_PATH + "?key=" + flow.get().getSecretKey());
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private Long extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Long l)
            return l;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
