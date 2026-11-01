package com.group17.game.DTOs;

import java.util.Locale;

// Public profile fields only; never expose a User entity to the player cards.
public class PlayerCard {
    private final Long id;
    private final String username;
    private final String avatar;

    public PlayerCard(Long id, String username, String avatar) {
        this.id = id;
        this.username = username;
        this.avatar = avatar;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }

    public String getAvatar() {
        if (avatar == null) return null;
        // Support Discord images and local profile images, without script URLs.
        if (avatar.startsWith("https://") || avatar.startsWith("http://")
                || avatar.startsWith("/images/")) return avatar;
        return null;
    }

    public String getInitials() {
        if (username == null || username.isBlank()) return "?";
        String name = username.strip();
        return name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }
}
