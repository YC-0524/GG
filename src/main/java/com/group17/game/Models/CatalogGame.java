
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
}

