package com.group17.game;

import java.util.List;
import java.util.Random;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.group17.game.Models.User;
import com.group17.game.Utilities.HashUtil;

public class Helpers {
    public static UsernamePasswordAuthenticationToken login(User user) {
        return UsernamePasswordAuthenticationToken.authenticated(user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public static User generateRandomValidTestUser() {
        Random random = new Random();

        User user = new User(
                "u" + random.nextInt(),
                "e" + random.nextInt() + "@gmail.com",
                HashUtil.generateRandomHash());

        return user;
    }
}
