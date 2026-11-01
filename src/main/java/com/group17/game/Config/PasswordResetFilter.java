package com.group17.game.Config;

import java.io.IOException;

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

            Long userId = (Long)auth.getPrincipal();
            if (userId == null) {
                return;
            }

            if (passwordFlowRepository.existsByUserIDAndActiveTrue(userId)) {
                ChangePasswordFlow flow = passwordFlowRepository.findByUserIDAndActiveTrue(userId);
                response.sendRedirect(request.getContextPath() + SET_PASSWORD_PATH + "?key=" + flow.getSecretKey());
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
