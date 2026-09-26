package exceptions;

/**
 * Thrown when an incident requires team dispatch but no compatible team is currently available.
 */
public class NoTeamAvailableException extends Exception {
    private final String incidentId;
    private final String disasterType;

    public NoTeamAvailableException(String incidentId, String disasterType, String message) {
        super(message);
        this.incidentId = incidentId;
        this.disasterType = disasterType;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getDisasterType() {
        return disasterType;
    }
}
