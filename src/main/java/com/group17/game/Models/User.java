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

@Entity
@Table(name = "users")
public class User {

    public static int MINIMUM_PASSWORD_LENGTH = 8;

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

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    private Instant timeoutUntil;
    
    // sha256 hashes are exactly 64 characters
    @Column(name = "password_hash")
    @Size(min = 64, max = 64)
    @NotBlank 
    private String passwordHash;
    
    @Column(name = "registered")
    private Date registered;

    // empty constructor, needed for springboot initializers
    public User() {}

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = UserRole.USER;
        this.registered = new Date();
    }

    // full constructor, has every non generated field
    public User(String username, String avatar, String email, String hash, Date registered, UserRole role) {
        this.username = username;
        this.avatar = avatar;
        this.email = email;
        this.passwordHash = hash;
        this.registered = registered;
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

    public UserRole getRole() {
        return  this.role;
    }

    // status getters
    public UserStatus getStatus() {
        return status;
    }

    public java.time.Instant getTimeoutUntil() {
        return timeoutUntil;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
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
}
