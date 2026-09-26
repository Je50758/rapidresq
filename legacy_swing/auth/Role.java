package auth;

import java.awt.Color;

/**
 * User roles with distinct authorization scopes and visual badge colors.
 * - ADMIN: Red badge (#DC2626)
 * - RESPONSE_TEAM: Blue badge (#2563EB)
 * - VOLUNTEER: Green badge (#16A34A)
 * - CITIZEN: Gray badge (#64748B)
 */
public enum Role {
    ADMIN("Administrator", new Color(220, 38, 38), new Color(254, 242, 242)),
    RESPONSE_TEAM("Response Team", new Color(37, 99, 235), new Color(239, 246, 255)),
    VOLUNTEER("Community Volunteer", new Color(22, 163, 74), new Color(240, 253, 244)),
    CITIZEN("Citizen Reporter", new Color(100, 116, 139), new Color(248, 250, 252));

    private final String displayName;
    private final Color badgeColor;
    private final Color badgeBackground;

    Role(String displayName, Color badgeColor, Color badgeBackground) {
        this.displayName = displayName;
        this.badgeColor = badgeColor;
        this.badgeBackground = badgeBackground;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    public Color getBadgeBackground() {
        return badgeBackground;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
