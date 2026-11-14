package com.group17.game.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "game_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_name"}))
public class GameEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    // "rank" is a SQL function name, so the column gets a safer name
    @Column(name = "game_rank", nullable = false)
    private String rank;

    @Column(name = "searchable", columnDefinition = "boolean not null default true")
    private boolean searchable = true;

    protected GameEntry() {}

    public GameEntry(User user, String gameName, String rank) {
        this.user = user;
        this.gameName = gameName;
        this.rank = rank;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getGameName() {
        return gameName;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }
    
    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }
}
