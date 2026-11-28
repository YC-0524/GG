package com.group17.game.Models;

// New SOS posts start as OPEN. More statuses (e.g. RESOLVED) can be added later.
public enum SosStatus {
    OPEN("Open");

    private final String label;

    SosStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}