package com.group17.game.Models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "recovery_attempts")
public class RecoveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id", unique = true)
    private long userID;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @Column(name = "last_failed_at")
    private Instant lastFailedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public RecoveryAttempt() {}

    public RecoveryAttempt(long userID) {
        this.userID = userID;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public long getId() {
        return id;
    }

    public long getUserID() {
        return userID;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Instant getLastFailedAt() {
        return lastFailedAt;
    }

    public void setLastFailedAt(Instant lastFailedAt) {
        this.lastFailedAt = lastFailedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
