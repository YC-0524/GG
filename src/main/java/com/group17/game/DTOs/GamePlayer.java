package com.group17.game.DTOs;

// One row on a game's page: the player's public card plus their rank in that game (US 22).
public class GamePlayer {
    private final PlayerCard player;
    private final String rank;

    public GamePlayer(PlayerCard player, String rank) {
        this.player = player;
        this.rank = rank;
    }

    public PlayerCard getPlayer() {
        return player;
    }

    public String getRank() {
        return rank;
    }
}
