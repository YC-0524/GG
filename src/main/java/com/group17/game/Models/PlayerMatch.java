package com.group17.game.Models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Two players who liked each other (US 47).
// userA always has the lower id, so each pair of players can only have one match.
@Entity
@Table(name = "player_matches", uniqueConstraints = @UniqueConstraint(columnNames = {"user_a_id", "user_b_id"}))
public class PlayerMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // when each player saw "It's a match!"
    private Instant userASeenAt;
    private Instant userBSeenAt;

    // last time each player read the chat, used for the unread badge (US 48)
    private Instant userAReadAt;
    private Instant userBReadAt;

    // set when either player unmatches (US 50); the chat is closed after this
    private Instant endedAt;
    private Long endedById;

    protected PlayerMatch() {}

    public PlayerMatch(User first, User second) {
        if (first.getId() < second.getId()) {
            this.userA = first;
            this.userB = second;
        } else {
            this.userA = second;
            this.userB = first;
        }
    }

    public boolean involves(long userId) {
        return userA.getId() == userId || userB.getId() == userId;
    }

    // the player in this match who isn't userId
    public User other(long userId) {
        return userA.getId() == userId ? userB : userA;
    }

    public boolean isActive() {
        return endedAt == null;
    }

    public Instant seenAt(long userId) {
        return userA.getId() == userId ? userASeenAt : userBSeenAt;
    }

    public void markSeen(long userId, Instant when) {
        if (userA.getId() == userId) {
            userASeenAt = when;
        } else {
            userBSeenAt = when;
        }
    }

    public void markRead(long userId, Instant when) {
        if (userA.getId() == userId) {
            userAReadAt = when;
        } else {
            userBReadAt = when;
        }
    }

    public void end(long userId, Instant when) {
        endedAt = when;
        endedById = userId;
    }

    public Long getId() {
        return id;
    }

    public User getUserA() {
        return userA;
    }

    public User getUserB() {
        return userB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Long getEndedById() {
        return endedById;
    }
}
