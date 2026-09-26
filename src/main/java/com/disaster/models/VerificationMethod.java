package com.disaster.models;

/**
 * The ten structured verification methods an administrator can run against a
 * reported incident before trusting / verifying it. Each method carries a
 * realistic weight - stronger confirmations count more toward auto-verification.
 */
public enum VerificationMethod {
    GPS_PINGS("GPS Pings", "🛰️", "Checking real-time coordinate logs against the reported location", 2),
    EMERGENCY_CONTACTS("Emergency Contacts", "📞", "Calling listed owners to verify distress", 2),
    DOUBLE_CALLING("Double-Calling", "☎️", "Re-dialing the reporting number to ensure authenticity", 1),
    RADAR_AUDITS("Radar Audits", "🌦️", "Checking weather feeds against reported storms", 2),
    SENSOR_MATCHED("Sensor Matched", "🎧", "Confirming acoustic signatures (e.g. gunshots, blasts)", 2),
    THERMAL_SCANS("Thermal Scans", "🛰️🔥", "Using satellites to check for heat/fire plumes", 3),
    MULTI_WITNESSING("Multi-Witnessing", "👥", "Matching multiple independent calls for the same issue", 2),
    USER_PROMPTS("User Prompts", "📱", "Requiring device screen taps to abort false crash alerts", 1),
    DRONE_SWEEPS("Drone Sweeps", "🚁", "Sending a UAV for rapid visual checks", 3),
    METHANE_LAYOUT("M/ETHANE Data Layout", "📋", "Grading the report using the M/ETHANE acronym checklist", 2);

    private final String displayName;
    private final String icon;
    private final String description;
    private final int weight; // verification points when the check passes

    VerificationMethod(String displayName, String icon, String description, int weight) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.weight = weight;
    }

    public String getDisplayName() { return displayName; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
    public int getWeight() { return weight; }
}
