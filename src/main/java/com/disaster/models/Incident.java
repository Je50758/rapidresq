package com.disaster.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract base entity representing a generic disaster incident.
 * Persisted using JPA SINGLE_TABLE inheritance strategy.
 * Implements Comparable to prioritize highest urgency incidents in PriorityQueues.
 * <p>
 * Tracks the set of distinct usernames that filed crowd confirmations so the same
 * account cannot inflate {@code reportCount} by confirming repeatedly.
 */
@Entity
@Table(name = "incidents")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "disaster_category", discriminatorType = DiscriminatorType.STRING)
public abstract class Incident implements Comparable<Incident> {
    @Id
    private String id;

    private String location;
    private LocalDateTime timestamp;
    private LocalDateTime resolvedTimestamp;
    private int injuredCount;
    private int reportCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_confirmations", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "confirmed_by")
    private Set<String> confirmedBy = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    private double priority;
    private String reporterUsername;
    private int reporterTrustScore;

    @Column(length = 2000)
    private String description;

    private String assignedTeamId;

    /** Report coordinates for proximity matching (shelters, nearby teams). */
    private double latitude;
    private double longitude;

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Incident() {
        this.timestamp = LocalDateTime.now();
        this.status = IncidentStatus.REPORTED;
    }

    public Incident(String id, String location, int injuredCount, Severity severity,
                    String reporterUsername, int reporterTrustScore, String description) {
        this.id = id;
        this.location = location;
        this.timestamp = LocalDateTime.now();
        this.injuredCount = Math.max(0, injuredCount);
        this.reportCount = 1;
        this.severity = severity != null ? severity : Severity.LOW;
        this.status = IncidentStatus.REPORTED;
        this.reporterUsername = reporterUsername != null ? reporterUsername : "anonymous";
        this.reporterTrustScore = reporterTrustScore;
        this.description = description != null ? description : "";
        this.assignedTeamId = null;
        this.resolvedTimestamp = null;
        this.priority = calculatePriority();
    }

    /**
     * Polymorphic method to calculate incident priority score.
     * Each disaster type implements its own formula based on specific threat factors.
     */
    public abstract double calculatePriority();

    /**
     * Returns the specific disaster category of the incident.
     */
    public abstract DisasterType getDisasterType();

    public void incrementReportCount() {
        this.reportCount++;
        this.priority = calculatePriority();
    }

    /**
     * Records a crowd confirmation from the given username. Returns false when this
     * account has already confirmed (one confirmation per user).
     */
    public boolean addConfirmation(String username) {
        if (username == null || username.isBlank()) return false;
        boolean added = confirmedBy.add(username);
        if (added) {
            incrementReportCount();
        }
        return added;
    }

    public boolean hasConfirmed(String username) {
        return username != null && confirmedBy.contains(username);
    }

    @Override
    public int compareTo(Incident other) {
        if (other == null) return -1;
        return Double.compare(other.priority, this.priority);
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(TIME_FORMATTER) : "N/A";
    }

    public LocalDateTime getResolvedTimestamp() {
        return resolvedTimestamp;
    }

    public void setResolvedTimestamp(LocalDateTime resolvedTimestamp) {
        this.resolvedTimestamp = resolvedTimestamp;
    }

    public String getFormattedResolvedTimestamp() {
        return resolvedTimestamp != null ? resolvedTimestamp.format(TIME_FORMATTER) : "N/A";
    }

    public int getInjuredCount() {
        return injuredCount;
    }

    public void setInjuredCount(int injuredCount) {
        this.injuredCount = Math.max(0, injuredCount);
        this.priority = calculatePriority();
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    /** Distinct usernames that have confirmed this incident (safe read-only view). */
    public Set<String> getConfirmedBy() {
        return Collections.unmodifiableSet(confirmedBy);
    }

    public void setConfirmedBy(Set<String> confirmedBy) {
        this.confirmedBy = confirmedBy != null ? new LinkedHashSet<>(confirmedBy) : new LinkedHashSet<>();
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
        this.priority = calculatePriority();
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
        if (status == IncidentStatus.RESOLVED && this.resolvedTimestamp == null) {
            this.resolvedTimestamp = LocalDateTime.now();
        }
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }

    public int getReporterTrustScore() {
        return reporterTrustScore;
    }

    public void setReporterTrustScore(int reporterTrustScore) {
        this.reporterTrustScore = reporterTrustScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedTeamId() {
        return assignedTeamId;
    }

    public void setAssignedTeamId(String assignedTeamId) {
        this.assignedTeamId = assignedTeamId;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incident)) return false;
        Incident incident = (Incident) o;
        return Objects.equals(id, incident.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s at %s | Severity: %s | Status: %s | Priority: %.1f | Reports: %d",
                id, getDisasterType(), location, severity != null ? severity.name() : "N/A",
                status != null ? status.name() : "N/A", priority, reportCount);
    }
}
