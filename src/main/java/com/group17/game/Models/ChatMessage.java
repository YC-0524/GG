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

// A chat message between two matched players (US 48)
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    public static final int MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private PlayerMatch playerMatch;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // up to 500 characters; the column is wider because some characters (like emoji) take two Java chars
    @Column(nullable = false, length = MAX_LENGTH * 2)
    private String body;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ChatMessage() {}

    public ChatMessage(PlayerMatch playerMatch, User sender, String body) {
        this.playerMatch = playerMatch;
        this.sender = sender;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public PlayerMatch getPlayerMatch() {
        return playerMatch;
    }

    public User getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
