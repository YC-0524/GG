package com.group17.game.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "catalog_games")
public class CatalogGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String gameName;

    @Column(unique = true)
    private Long rawgId;

    @Column(length = 1000)
    private String coverUrl;

    @Column(length = 500)
    private String genres;

    @Column(length = 500)
    private String platforms;

    protected CatalogGame() {}

    public CatalogGame(String gameName) {
        this.gameName = gameName;
    }

    public Long getId() {
        return id;
    }

    public String getGameName() {
        return gameName;
    }

    public Long getRawgId() {
        return rawgId;
    }

    public void setRawgId(Long rawgId) {
        this.rawgId = rawgId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String platforms) {
        this.platforms = platforms;
    }
}

