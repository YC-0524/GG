package com.group17.game.Utilities;

import java.util.Optional;
import java.util.regex.Pattern;

public class PasswordPolicy {
    // Pattern regex: a library that is used for describing text patterns
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[^A-Za-z0-9\\s]");

    private PasswordPolicy() {}

    public static Optional<String> validate(String password, String confirmPassword) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (!UPPER.matcher(password).find()) {
            return Optional.of("Password needs an uppercase letter.");
        }
        if (!LOWER.matcher(password).find()) {
            return Optional.of("Password needs a lowercase letter.");
        }
        if (!NUMBER.matcher(password).find()) {
            return Optional.of("Password needs a number.");
        }
        if (!SYMBOL.matcher(password).find()) {
            return Optional.of("Password needs a symbol.");
        }
        if (!password.equals(confirmPassword)) {
            return Optional.of("Passwords don't match.");
        }
        return Optional.empty();
    }
}
