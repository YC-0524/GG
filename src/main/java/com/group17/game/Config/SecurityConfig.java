package com.group17.game.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.group17.game.Models.ChangePasswordFlowRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            GoogleLoginSuccessHandler googleLoginSuccessHandler,
            ChangePasswordFlowRepository passwordFlowRepository)
            throws Exception {

        http
                // Allow login, registration, OAuth, and error pages without authentication.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/error",
                                "/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        // Require authentication for all other requests.
                        .anyRequest().authenticated()
                )
                // Configure Google OAuth login.
                .oauth2Login(oauth -> oauth
                        .successHandler(googleLoginSuccessHandler)
                )
                // Add the password-reset filter.
                .addFilterBefore(
                        new PasswordResetFilter(passwordFlowRepository),
                        UsernamePasswordAuthenticationFilter.class
                )
                // Configure logout.
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}