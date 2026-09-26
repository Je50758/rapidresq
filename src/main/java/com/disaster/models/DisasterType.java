package com.disaster.models;

/**
 * Categories of disasters supported by the system.
 */
public enum DisasterType {
    FIRE("Fire Outbreak", "🔥"),
    FLOOD("Water Flood & Waterlogging", "🌊"),
    ACCIDENT("Road / Transport Accident", "🚗"),
    EARTHQUAKE("Earthquake & Structural Collapse", "🏚️");

    private final String title;
    private final String icon;

    DisasterType(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return icon + " " + title;
    }
}
