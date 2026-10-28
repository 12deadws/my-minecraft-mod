package com.caelestis.client.module;

public enum Category {
    COMBAT("Combat"),
    PLAYER("Player"),
    CRYSTAL("Crystal PvP"),
    RENDER("Render"),
    SETTINGS("Settings");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
