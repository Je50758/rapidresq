package com.disaster.observers;

import com.disaster.models.Incident;
import com.disaster.models.ResponseTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete Observer that logs all incident lifecycle events to an in-memory audit log
 * and system output console.
 */
@Component
public class StatusLogger implements IncidentObserver {
    private static final Logger log = LoggerFactory.getLogger(StatusLogger.class);
    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final String eventType;
        private final String incidentId;
        private final String details;

        public LogEntry(String eventType, String incidentId, String details) {
            this.timestamp = LocalDateTime.now();
            this.eventType = eventType;
            this.incidentId = incidentId;
            this.details = details;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getEventType() {
            return eventType;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public String getDetails() {
            return details;
        }

        public String getFormattedTimestamp() {
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Override
        public String toString() {
            return String.format("[%s] [%s] (Incident: %s) -> %s",
                    getFormattedTimestamp(), eventType, incidentId, details);
        }
    }

    private final List<LogEntry> logs = new ArrayList<>();

    @Override
    public void onIncidentReported(Incident incident) {
        log("REPORTED", incident.getId(), "Incident reported at " + incident.getLocation() +
                " | Severity: " + incident.getSeverity() + " | Initial Priority: " + incident.getPriority());
    }

    @Override
    public void onIncidentVerified(Incident incident) {
        log("VERIFIED", incident.getId(), "Incident crowd-verified (" + incident.getReportCount() +
                " reports). Priority recalculated to: " + incident.getPriority());
    }

    @Override
    public void onTeamAssigned(Incident incident, ResponseTeam team) {
        log("TEAM_ASSIGNED", incident.getId(), "Assigned team: " + team.getName() +
                " (" + team.getSpecialization() + ") | Base: " + team.getBaseStation());
    }

    @Override
    public void onIncidentResolved(Incident incident) {
        log("RESOLVED", incident.getId(), "Incident cleared at " + incident.getLocation() +
                ". Assigned teams released.");
    }

    private synchronized void log(String eventType, String incidentId, String details) {
        LogEntry entry = new LogEntry(eventType, incidentId, details);
        logs.add(entry);
        log.info("📝 [AUDIT LOG] {}", entry);
    }

    /** Public entry point for operational events outside the incident observer flow
     *  (signals, verification checks, broadcasts, geofence alarms, demobilizations). */
    public void logExternal(String eventType, String incidentId, String details) {
        log(eventType, incidentId, details);
    }

    public synchronized List<LogEntry> getLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }
}
