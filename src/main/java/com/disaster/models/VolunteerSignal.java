package com.disaster.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A directional signal sent by an administrator to a volunteer unit (or volunteer user)
 * - e.g. deployment confirmation, standby order, stand-down. Volunteers see the signal
 * in their inbox and acknowledge it.
 */
@Entity
@Table(name = "volunteer_signals")
public class VolunteerSignal {
    public enum SignalType {
        DEPLOYMENT_CONFIRMED("✅ Deployment Confirmed"),
        STANDBY("🟡 Standby Order"),
        MOVE_TO_LOCATION("📍 Move To Location"),
        STAND_DOWN("🔵 Stand Down"),
        BROADCAST("📢 Broadcast Alert");

        private final String label;
        SignalType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum SignalStatus {
        SENT, ACKNOWLEDGED
    }

    @Id
    private String signalId;

    @Enumerated(EnumType.STRING)
    private SignalType signalType;

    /** Target volunteer team id (e.g. TEAM-VOL01), or null when targeting a single user. */
    private String targetTeamId;

    /** Target volunteer username when the signal is for one person, else null. */
    private String targetUsername;

    private String incidentId;
    @Column(length = 1000)
    private String message;
    private String location;

    @Enumerated(EnumType.STRING)
    private SignalStatus status;

    private LocalDateTime sentAt;
    private LocalDateTime acknowledgedAt;
    private String sentByAdmin;

    public VolunteerSignal() {
        this.sentAt = LocalDateTime.now();
        this.status = SignalStatus.SENT;
    }

    public VolunteerSignal(String signalId, SignalType signalType, String targetTeamId, String targetUsername,
                           String incidentId, String message, String location, String sentByAdmin) {
        this.signalId = signalId;
        this.signalType = signalType;
        this.targetTeamId = targetTeamId;
        this.targetUsername = targetUsername;
        this.incidentId = incidentId;
        this.message = message;
        this.location = location;
        this.sentByAdmin = sentByAdmin;
        this.sentAt = LocalDateTime.now();
        this.status = SignalStatus.SENT;
    }

    public String getFormattedSentAt() {
        return sentAt != null ? sentAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    public String getFormattedAcknowledgedAt() {
        return acknowledgedAt != null ? acknowledgedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
    }

    // --- Getters & Setters ---
    public String getSignalId() { return signalId; }
    public void setSignalId(String signalId) { this.signalId = signalId; }

    public SignalType getSignalType() { return signalType; }
    public void setSignalType(SignalType signalType) { this.signalType = signalType; }

    public String getTargetTeamId() { return targetTeamId; }
    public void setTargetTeamId(String targetTeamId) { this.targetTeamId = targetTeamId; }

    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public SignalStatus getStatus() { return status; }
    public void setStatus(SignalStatus status) { this.status = status; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getSentByAdmin() { return sentByAdmin; }
    public void setSentByAdmin(String sentByAdmin) { this.sentByAdmin = sentByAdmin; }
}
