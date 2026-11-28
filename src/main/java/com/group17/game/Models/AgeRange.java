package com.group17.game.Models;

public enum AgeRange {
    TEEN("13–17"),
    AGE_18_24("18–24"),
    AGE_25_34("25–34"),
    AGE_35_PLUS("35+");

    private final String label;

    AgeRange(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
