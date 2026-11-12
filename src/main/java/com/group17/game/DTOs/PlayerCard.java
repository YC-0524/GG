package com.group17.game.DTOs;

import java.util.ArrayList;
import java.util.Locale;

import com.group17.game.Models.ActivityStatus;
import com.group17.game.Models.GameEntry;
import com.group17.game.Models.ProfileTag;

import jakarta.annotation.Nullable;

// Public profile fields only; never expose a User entity to the player cards.
public class PlayerCard {
    private final Long id;
    private final String username;
    private final String avatar;
    private ActivityStatus activityStatus;
    private ArrayList<ProfileTag> tags;
    private ArrayList<GameEntry> games;

    public PlayerCard(
        Long id, 
        String username, 
        String avatar, 
        ActivityStatus activityStatus,
        @Nullable ArrayList<ProfileTag> tags,
        @Nullable ArrayList<GameEntry> gameEntries
    ) {
        this.id = id;
        this.username = username;
        this.avatar = avatar;
        this.activityStatus = activityStatus;
        this.games = gameEntries;
        if (tags == null) {
            this.tags = new ArrayList<>();
        } else {
            this.tags = tags;
        }
    }

    public void setProfileTags(ArrayList<ProfileTag> tags) {
        this.tags = tags;
    }

    public void setGameEntries(ArrayList<GameEntry> gameEntries) {
        this.games = gameEntries;
    }

    public void addProfileTag(ProfileTag tag) {
        this.tags.add(tag);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public ActivityStatus getActivityStatus() { return this.activityStatus; }

    public String getAvatar() {
        if (avatar == null) return null;
        // Support Discord images and local profile images, without script URLs.
        if (avatar.startsWith("https://") || avatar.startsWith("http://")
                || avatar.startsWith("/images/")) return avatar;
        return null;
    }

    public ArrayList<ProfileTag> getProfileTags() {
        return this.tags;
    }

    public ArrayList<GameEntry> getGames() {
        return this.games;
    }

    public String getInitials() {
        if (username == null || username.isBlank()) return "?";
        String name = username.strip();
        return name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }
}
