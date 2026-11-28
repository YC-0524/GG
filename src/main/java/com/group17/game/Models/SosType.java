package com.group17.game.Models;

// types of help
public enum SosType {
    SINGLE_PLAYER_TIPS("Single-player tips"),
    MULTIPLAYER_GUIDANCE("Multiplayer guidance"),
    NEED_TEAMMATE_NOW("Need a teammate now");

    private final String label;

    SosType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}