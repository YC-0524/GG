package com.group17.game.Models;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "player_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"sender_id", "target_id"}))
public class PlayerLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PlayerLike() {}

    public PlayerLike(User sender, User target) {
        this.sender = sender;
        this.target = target;
    }

    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public User getTarget() {
        return target;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
