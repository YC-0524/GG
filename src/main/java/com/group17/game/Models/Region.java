package com.group17.game.Models;

public enum Region {
    NA_WEST("NA West"),
    NA_EAST("NA East"),
    EUROPE("Europe"),
    ASIA("Asia"),
    OCEANIA("Oceania"),
    SOUTH_AMERICA("South America");

    private final String label;

    Region(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
