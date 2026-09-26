package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a simulated automated escalation record
 * to the National Emergency Service (999) and Armed Forces Disaster Wing.
 */
public class EscalationLog {
    private final String escalationId;
    private final String incidentId;
    private final DisasterType disasterType;
    private final String location;
    private final double priority;
    private final LocalDateTime timestamp;
    private final String simulatedAgency; // e.g., "National Emergency Service (999)"
    private final String transmissionStatus; // e.g., "DISPATCHED_TO_NATIONAL_GRID"

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EscalationLog(String escalationId, String incidentId, DisasterType disasterType,
                         String location, double priority) {
        this.escalationId = escalationId;
        this.incidentId = incidentId;
        this.disasterType = disasterType;
        this.location = location;
        this.priority = priority;
        this.timestamp = LocalDateTime.now();
        this.simulatedAgency = "National Emergency Service (999) & Disaster Control Room";
        this.transmissionStatus = "BROADCAST_CONFIRMED";
    }

    public String getEscalationId() {
        return escalationId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public String getLocation() {
        return location;
    }

    public double getPriority() {
        return priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public String getSimulatedAgency() {
        return simulatedAgency;
    }

    public String getTransmissionStatus() {
        return transmissionStatus;
    }

    @Override
    public String toString() {
        return String.format("[%s] 🚨 Escalated to %s at %s | Incident: %s (%s at %s) | Priority: %.1f",
                escalationId, simulatedAgency, getFormattedTimestamp(), incidentId, disasterType, location, priority);
    }
}
