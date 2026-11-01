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
    public SecurityFilterChain filterChain(HttpSecurity http, ChangePasswordFlowRepository passwordFlowRepository) throws Exception {
        // permit certain requests depending on the current auth state
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new PasswordResetFilter(passwordFlowRepository), UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));
        return http.build();
    }
}
