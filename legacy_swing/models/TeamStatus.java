package models;

/**
 * Status of a response team in the deployment cycle.
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
