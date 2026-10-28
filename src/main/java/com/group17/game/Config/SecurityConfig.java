package com.group17.game.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // permit certain requests depending on the current auth state
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/profile/**",
                                "/login.html", "/register.html", "/**",
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
