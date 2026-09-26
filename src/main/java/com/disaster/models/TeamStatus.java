package com.disaster.models;

/**
 * Status of an emergency response team.
 */
public enum TeamStatus {
    AVAILABLE("Available for Deployment"),
    ASSIGNED("Dispatched to Incident"),
    BUSY("Engaged in Operations"),
    STANDBY("On Standby / Maintenance");

    private final String description;

    TeamStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
