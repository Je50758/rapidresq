package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Abstract base class representing a generic disaster incident.
 * Implements Comparable to prioritize highest urgency incidents in PriorityQueues.
 */
public abstract class Incident implements Comparable<Incident> {
    private final String id;
    private String location;
    private final LocalDateTime timestamp;
    private LocalDateTime resolvedTimestamp;
    private int injuredCount;
    private int reportCount;
    private Severity severity;
    private IncidentStatus status;
    private double priority;
    private String reporterUsername;
    private int reporterTrustScore;
    private String description;
    private String assignedTeamId;

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Incident(String id, String location, int injuredCount, Severity severity, 
                    String reporterUsername, int reporterTrustScore, String description) {
        this.id = id;
        this.location = location;
        this.timestamp = LocalDateTime.now();
        this.injuredCount = Math.max(0, injuredCount);
        this.reportCount = 1; // Initial report
        this.severity = severity != null ? severity : Severity.LOW;
        this.status = IncidentStatus.REPORTED;
        this.reporterUsername = reporterUsername != null ? reporterUsername : "anonymous";
        this.reporterTrustScore = reporterTrustScore;
        this.description = description != null ? description : "";
        this.assignedTeamId = null;
        this.resolvedTimestamp = null;
        
        // Initial priority calculation via polymorphic hook
        this.priority = calculatePriority();
    }

    /**
     * Polymorphic method to calculate incident priority score.
     * Each disaster type implements its own formula based on specific threat factors.
     * @return Calculated priority score (higher score = more urgent)
     */
    public abstract double calculatePriority();

    /**
     * Returns the specific disaster category of the incident.
     */
    public abstract DisasterType getDisasterType();

    /**
     * Increment report count from citizen confirmation and re-evaluate status/priority.
     */
    public void incrementReportCount() {
        this.reportCount++;
        // Recalculate priority as crowd verification increases confidence
        this.priority = calculatePriority();
    }

    /**
     * Comparable implementation: Descending order so highest priority appears at the head of PriorityQueue.
     */
    @Override
    public int compareTo(Incident other) {
        if (other == null) return -1;
        // Higher priority first
        return Double.compare(other.priority, this.priority);
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
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

    public String getFormattedTimestamp() {
        return timestamp.format(TIME_FORMATTER);
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
                id, getDisasterType(), location, severity.name(), status.name(), priority, reportCount);
    }
}
