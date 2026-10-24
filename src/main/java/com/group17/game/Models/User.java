package com.group17.game.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Date;

@Entity
@Table(name = "users")
public class User {
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
    private String email;
    
    // sha256 hashes are exactly 64 characters
    @Column(name = "password_hash")
    @Size(min = 64,max = 64)
    @NotBlank 
    private String password_hash;
    
    @Column(name = "registered")
    private Date registered;

    public User() {}

    public User(String username, String avatar, String email, String hash, Date registered) {
        this.username = username;
        this.avatar = avatar;
        this.email = email;
        this.password_hash = hash;
        this.registered = registered;
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

    public String getPassword_hash() {
        return password_hash;
    }

    public Date getRegistered() {
        return registered;
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

    public void setPassword_hash(String password_hash) {
        this.password_hash = password_hash;
    }

    public void setRegistered(Date registered) {
        this.registered = registered;
    }
}
