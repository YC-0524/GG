package com.group17.game.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscordUserResponse {
    private String id;
    private String username;
    private String email;
    private String discriminator;
    private String avatar;
    private boolean bot;
    private boolean system;
    private boolean mfaEnabled;

    @JsonProperty("mfa_enabled")
    private boolean mfaEnabledJson;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDiscriminator() { return discriminator; }
    public void setDiscriminator(String discriminator) { this.discriminator = discriminator; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public boolean isBot() { return bot; }
    public void setBot(boolean bot) { this.bot = bot; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public boolean isMfaEnabledJson() { return mfaEnabledJson; }
    public void setMfaEnabledJson(boolean mfaEnabledJson) { this.mfaEnabledJson = mfaEnabledJson; }
}
