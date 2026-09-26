package com.disaster.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Post-operation report generated automatically when an incident is resolved.
 * Summarizes the response and lists currently AVAILABLE resources matched to
 * the disaster type requirement, so coordinators know what remains deployable.
 */
@Entity
@Table(name = "mission_reports")
public class MissionReport {
    @Id
    private String reportId;

    private String incidentId;
    private String location;

    @Enumerated(EnumType.STRING)
    private DisasterType disasterType;

    @Column(length = 2000)
    private String summary;

    private int totalInjuredHandled;
    private int teamsDeployedCount;
    private int volunteerSignalsSent;
    private int verificationChecksPassed;

    /**
     * JSON string snapshot of requirement-matched AVAILABLE resources at resolve time.
     * (Stored as text to keep the entity self-contained; parsed on read for the UI.)
     */
    @Column(length = 4000)
    private String availableResourcesJson;

    private String generatedBy;
    private LocalDateTime generatedAt;

    public MissionReport() {
        this.generatedAt = LocalDateTime.now();
    }

    public MissionReport(String reportId, String incidentId, String location, DisasterType disasterType,
                         String summary, int totalInjuredHandled, int teamsDeployedCount,
                         int volunteerSignalsSent, int verificationChecksPassed,
                         String availableResourcesJson, String generatedBy) {
        this.reportId = reportId;
        this.incidentId = incidentId;
        this.location = location;
        this.disasterType = disasterType;
        this.summary = summary;
        this.totalInjuredHandled = totalInjuredHandled;
        this.teamsDeployedCount = teamsDeployedCount;
        this.volunteerSignalsSent = volunteerSignalsSent;
        this.verificationChecksPassed = verificationChecksPassed;
        this.availableResourcesJson = availableResourcesJson;
        this.generatedBy = generatedBy;
        this.generatedAt = LocalDateTime.now();
    }

    public String getFormattedGeneratedAt() {
        return generatedAt != null ? generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    // --- Getters & Setters ---
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public DisasterType getDisasterType() { return disasterType; }
    public void setDisasterType(DisasterType disasterType) { this.disasterType = disasterType; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getTotalInjuredHandled() { return totalInjuredHandled; }
    public void setTotalInjuredHandled(int totalInjuredHandled) { this.totalInjuredHandled = totalInjuredHandled; }

    public int getTeamsDeployedCount() { return teamsDeployedCount; }
    public void setTeamsDeployedCount(int teamsDeployedCount) { this.teamsDeployedCount = teamsDeployedCount; }

    public int getVolunteerSignalsSent() { return volunteerSignalsSent; }
    public void setVolunteerSignalsSent(int volunteerSignalsSent) { this.volunteerSignalsSent = volunteerSignalsSent; }

    public int getVerificationChecksPassed() { return verificationChecksPassed; }
    public void setVerificationChecksPassed(int verificationChecksPassed) { this.verificationChecksPassed = verificationChecksPassed; }

    public String getAvailableResourcesJson() { return availableResourcesJson; }
    public void setAvailableResourcesJson(String availableResourcesJson) { this.availableResourcesJson = availableResourcesJson; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
