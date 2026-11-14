package com.group17.game.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.group17.game.Utilities.UserActivityService;

import java.io.IOException;

@Component
public class UserActivityFilter extends OncePerRequestFilter {

    private final UserActivityService userActivityService;

    public UserActivityFilter(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            Long userId = (Long)authentication.getPrincipal();

            userActivityService.updateLastSeen(userId);
        }

        filterChain.doFilter(request, response);
    }
}