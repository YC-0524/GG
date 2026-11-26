package com.group17.game.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Date;
import java.time.Instant;

import java.time.Duration;

@Entity
@Table(name = "users")
public class User {

    public static int MINIMUM_PASSWORD_LENGTH = 8;
    public static final Duration USERNAME_CHANGE_COOLDOWN = Duration.ofDays(30);
    public static final Duration AVATAR_CHANGE_COOLDOWN = Duration.ofDays(1);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "username")
    @NotBlank
    private String username;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "email", unique = true)
    @Email
    @NotBlank
    private String email;

    // essentially bakes in the standard dashboard
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role = UserRole.USER;

    private Instant timeoutUntil;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    private ActivityStatus activityStatus = ActivityStatus.ONLINE;

    @Column(name = "manual_status_override", columnDefinition = "boolean not null default false")
    private boolean manualStatusOverride = false;

    // sha256 hashes are exactly 64 characters
    @Column(name = "password_hash")
    @Size(min = 64, max = 64)
    @NotBlank
    private String passwordHash;

    @Column(name = "registered")
    private Date registered;

    @Column(name = "last_seen")
    private Date lastSeen;

    // Last time the player changed their own username
    // Moderator resets don't set this
    @Column(name = "username_changed_at")
    private Instant usernameChangedAt;

    // Last time the player uploaded their own profile picture
    // Moderator resets don't set this
    @Column(name = "avatar_changed_at")
    private Instant avatarChangedAt;

    // empty constructor, needed for springboot initializers
    public User() {
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = UserRole.USER;
        this.registered = new Date();
        this.lastSeen = new Date();
    }

    // full constructor, has every non generated field
    public User(String username, String avatar, String email, String hash, Date registered, Date lastSeen,
            UserRole role) {
        this.username = username;
        this.avatar = avatar;
        this.email = email;
        this.passwordHash = hash;
        this.registered = registered;
        this.lastSeen = lastSeen;
        this.role = role;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Date getRegistered() {
        return registered;
    }

    public Date getLastSeen() {
        return this.lastSeen;
    }

    public UserRole getRole() {
        return this.role;
    }

    // status getters
    public UserStatus getStatus() {
        return status;
    }

    public java.time.Instant getTimeoutUntil() {
        return timeoutUntil;
    }

    public ActivityStatus getActivityStatus() {
        return this.activityStatus;
    }

    public boolean isManualStatusOverride() {
        return manualStatusOverride;
    }

    public void setManualStatusOverride(boolean manualStatusOverride) {
        this.manualStatusOverride = manualStatusOverride;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setActivityStatus(ActivityStatus newStatus) {
        this.activityStatus = newStatus;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    public void setLastSeen(Date seen) {
        this.lastSeen = seen;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // status setters
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void setTimeoutUntil(java.time.Instant timeoutUntil) {
        this.timeoutUntil = timeoutUntil;
    }
    
    public Instant getUsernameChangedAt() {
        return usernameChangedAt;
    }

    public void setUsernameChangedAt(Instant usernameChangedAt) {
        this.usernameChangedAt = usernameChangedAt;
    }

    // null if the player has never changed their username
    public Instant getNextUsernameChangeAt() {
        return usernameChangedAt == null ? null : usernameChangedAt.plus(USERNAME_CHANGE_COOLDOWN);
    }

    public boolean canChangeUsername(Instant now) {
        Instant next = getNextUsernameChangeAt();
        return next == null || !now.isBefore(next);
    }

    public Instant getAvatarChangedAt() {
        return avatarChangedAt;
    }

    public void setAvatarChangedAt(Instant avatarChangedAt) {
        this.avatarChangedAt = avatarChangedAt;
    }

    // null if the player has never uploaded a profile picture
    public Instant getNextAvatarChangeAt() {
        return avatarChangedAt == null ? null : avatarChangedAt.plus(AVATAR_CHANGE_COOLDOWN);
    }

    public boolean canChangeAvatar(Instant now) {
        Instant next = getNextAvatarChangeAt();
        return next == null || !now.isBefore(next);
    }

    // first letter of the username, shown when there's no profile picture
    public String getInitial() {
        if (username == null || username.isBlank()) {
            return "?";
        }
        String name = username.strip();
        return name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
    }
}
