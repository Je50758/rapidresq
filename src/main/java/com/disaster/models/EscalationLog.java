package com.disaster.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a simulated automated escalation record
 * to the National Emergency Service (999) and Armed Forces Disaster Wing.
 */
@Entity
@Table(name = "escalation_logs")
public class EscalationLog {
    @Id
    private String escalationId;
    private String incidentId;

    @Enumerated(EnumType.STRING)
    private DisasterType disasterType;

    private String location;
    private double priority;
    private LocalDateTime timestamp;
    private String simulatedAgency;
    private String transmissionStatus;

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EscalationLog() {
        this.timestamp = LocalDateTime.now();
        this.simulatedAgency = "National Emergency Service (999) & Disaster Control Room";
        this.transmissionStatus = "BROADCAST_CONFIRMED";
    }

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

    public void setEscalationId(String escalationId) {
        this.escalationId = escalationId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "N/A";
    }

    public String getSimulatedAgency() {
        return simulatedAgency;
    }

    public void setSimulatedAgency(String simulatedAgency) {
        this.simulatedAgency = simulatedAgency;
    }

    public String getTransmissionStatus() {
        return transmissionStatus;
    }

    public void setTransmissionStatus(String transmissionStatus) {
        this.transmissionStatus = transmissionStatus;
    }

    @Override
    public String toString() {
        return String.format("[%s] 🚨 Escalated to %s at %s | Incident: %s (%s at %s) | Priority: %.1f",
                escalationId, simulatedAgency, getFormattedTimestamp(), incidentId, disasterType, location, priority);
    }
}
