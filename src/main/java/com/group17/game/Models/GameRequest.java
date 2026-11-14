
package com.group17.game.Models;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "game_requests")
public class GameRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String gameName;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private String gameMode;

    private Integer minPlayers;

    private Integer maxPlayers;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long requestedById;

    @Column(nullable = false)
    private String requestedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected GameRequest() {}

    public GameRequest(String gameName, String genre, String gameMode,
            Integer minPlayers, Integer maxPlayers, String description,
            Long requestedById, String requestedByUsername) {
        this.gameName = gameName;
        this.genre = genre;
        this.gameMode = gameMode;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.description = description;
        this.requestedById = requestedById;
        this.requestedByUsername = requestedByUsername;
    }

    public Long getId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public String getGenre() {
        return genre;
    }

    public String getGameMode() {
        return gameMode;
    }

    public Integer getMinPlayers() {
        return minPlayers;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public String getDescription() {
        return description;
    }

    public Long getRequestedById() {
        return requestedById;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

