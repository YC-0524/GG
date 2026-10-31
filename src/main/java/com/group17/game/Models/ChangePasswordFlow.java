package com.group17.game.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Date;

@Entity
@Table(name = "change_password_flows")
public class ChangePasswordFlow {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userID;

    @Column(name = "active")
    private boolean active;

    @Column(name = "secret_key")
    @Size(min = 64, max = 64)
    @NotBlank
    private String secretKey;
    
    @Column(name = "created")
    private Date created;

    // empty constructor, needed for springboot initializers
    public ChangePasswordFlow() {}

    // initialize date in constructor
    public ChangePasswordFlow(long userID, boolean active, String secretKey) {
        this.userID = userID;
        this.active = active;
        this.secretKey = secretKey;
        this.created = new Date();
    }

    public ChangePasswordFlow(long userID, boolean active, String secretKey, Date date) {
        this.userID = userID;
        this.active = active;
        this.secretKey = secretKey;
        this.created = date;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getID() {
        return this.id;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public long getUserID() {
        return this.userID;
    }
    
    public Date getCreated() {
        return this.created;
    }

    public boolean isActive() {
        return this.active;
    }
}
