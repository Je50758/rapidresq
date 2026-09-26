package com.disaster.models;

/**
 * User roles with distinct authorization scopes and visual styles.
 * - ADMIN
 * - RESPONSE_TEAM
 * - VOLUNTEER
 * - CITIZEN
 */
public enum Role {
    ADMIN("Administrator", "#DC2626", "#FEF2F2"),
    RESPONSE_TEAM("Response Team", "#2563EB", "#EFF6FF"),
    VOLUNTEER("Community Volunteer", "#16A34A", "#F0FDF4"),
    CITIZEN("Citizen Reporter", "#64748B", "#F8FAFC");

    private final String displayName;
    private final String colorHex;
    private final String bgHex;

    Role(String displayName, String colorHex, String bgHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.bgHex = bgHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getBgHex() {
        return bgHex;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
