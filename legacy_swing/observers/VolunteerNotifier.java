package observers;

import models.Incident;
import models.ResponseTeam;
import models.Severity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete Observer that pushes rapid deployment and verification alerts
 * directly to the community volunteer network.
 */
public class VolunteerNotifier implements IncidentObserver {
    public static class VolunteerAlert {
        private final String alertId;
        private final String incidentId;
        private final String message;
        private final LocalDateTime timestamp;
        private final Severity severity;

        public VolunteerAlert(String alertId, String incidentId, String message, Severity severity) {
            this.alertId = alertId;
            this.incidentId = incidentId;
            this.message = message;
            this.timestamp = LocalDateTime.now();
            this.severity = severity;
        }

        public String getAlertId() {
            return alertId;
        }

        public String getIncidentId() {
            return incidentId;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return String.format("[%s] ALERT: %s (Incident: %s | %s)",
                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), message, incidentId, severity);
        }
    }

    private final List<VolunteerAlert> activeAlerts = new ArrayList<>();
    private int alertSequence = 1;

    @Override
    public void onIncidentReported(Incident incident) {
        String msg = "New " + incident.getDisasterType() + " reported at " + incident.getLocation() + 
                     ". Volunteers in this sector please be on standby for ground verification!";
        addAlert(incident.getId(), msg, incident.getSeverity());
    }

    @Override
    public void onIncidentVerified(Incident incident) {
        String msg = "Incident " + incident.getId() + " at " + incident.getLocation() + 
                     " is VERIFIED by multiple citizens. Immediate first-aid & evacuation assistance needed!";
        addAlert(incident.getId(), msg, incident.getSeverity());
    }

    @Override
    public void onTeamAssigned(Incident incident, ResponseTeam team) {
        String msg = "Team " + team.getName() + " assigned to incident " + incident.getId() + 
                     ". Local volunteers assist with traffic clearance & staging.";
        addAlert(incident.getId(), msg, incident.getSeverity());
    }

    @Override
    public void onIncidentResolved(Incident incident) {
        String msg = "Incident " + incident.getId() + " at " + incident.getLocation() + 
                     " marked RESOLVED. Thank you community responders!";
        addAlert(incident.getId(), msg, incident.getSeverity());
    }

    private synchronized void addAlert(String incidentId, String message, Severity severity) {
        VolunteerAlert alert = new VolunteerAlert("ALERT-" + (alertSequence++), incidentId, message, severity);
        activeAlerts.add(alert);
        System.out.println("📢 [VOLUNTEER NOTIFICATION DISPATCHED] " + alert);
    }

    public synchronized List<VolunteerAlert> getActiveAlerts() {
        return Collections.unmodifiableList(new ArrayList<>(activeAlerts));
    }

    public synchronized void clearAlerts() {
        activeAlerts.clear();
    }
}
