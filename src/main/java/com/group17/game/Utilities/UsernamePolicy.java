package com.group17.game.Utilities;

import java.util.Optional;
import java.util.regex.Pattern;

public class UsernamePolicy {
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 20;
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9_]+");

    private UsernamePolicy() {}

    public static Optional<String> validate(String username) {
        if (username == null || username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
            return Optional.of("Username must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters.");
        }
        if (!ALLOWED.matcher(username).matches()) {
            return Optional.of("Username can only use letters, numbers and underscores.");
        }
        return Optional.empty();
    }
}