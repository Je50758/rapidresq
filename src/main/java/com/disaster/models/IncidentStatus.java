package com.disaster.models;

/**
 * Lifecycle status of an incident report:
 * REPORTED -> VERIFIED -> IN_PROGRESS -> RESOLVED
 */
public enum IncidentStatus {
    REPORTED("Reported (Unverified)"),
    VERIFIED("Verified by Citizens"),
    IN_PROGRESS("Response Team Dispatched"),
    RESOLVED("Resolved & Cleared");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
