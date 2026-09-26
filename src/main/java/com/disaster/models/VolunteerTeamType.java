package com.disaster.models;

/**
 * Structured volunteer team categories a citizen can join at registration.
 * Each type maps to the kinds of field assistance the unit provides.
 */
public enum VolunteerTeamType {
    MEDICAL_AID("Medical Aid Volunteer", "🩺", "First aid, triage support & medicine distribution"),
    RESCUE ("Rescue & Evacuation Volunteer", "🛟", "Boat/rope rescue, evacuation & shelter help"),
    LOGISTICS("Logistics & Relief Volunteer", "📦", "Relief packing, transport & supply distribution"),
    COMMUNICATION("Communication & Data Volunteer", "📡", "Reporting, verification calls & coordination"),
    FIRE_SUPPORT("Fire Support Volunteer", "🧯", "Fire-line assistance, hydration & crowd control");

    private final String displayName;
    private final String icon;
    private final String description;

    VolunteerTeamType(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
}
