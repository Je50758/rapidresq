package observers;

import models.DisasterType;
import models.EscalationLog;
import models.Incident;
import models.ResponseTeam;
import models.Severity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete Observer that simulates emergency escalation to the
 * National Emergency Service (999) whenever a CRITICAL severity incident is lodged.
 */
public class EmergencyEscalationNotifier implements IncidentObserver {
    private final List<EscalationLog> escalationLogs = new ArrayList<>();
    private int escalationCounter = 1;

    @Override
    public void onIncidentReported(Incident incident) {
        checkAndEscalate(incident, "INITIAL_CRITICAL_REPORT");
    }

    @Override
    public void onIncidentVerified(Incident incident) {
        checkAndEscalate(incident, "HIGH_SEVERITY_VERIFIED");
    }

    @Override
    public void onTeamAssigned(Incident incident, ResponseTeam team) {
        // No automatic 999 escalation upon routine team assignment
    }

    @Override
    public void onIncidentResolved(Incident incident) {
        // De-escalate or clear notification
    }

    private synchronized void checkAndEscalate(Incident incident, String triggerReason) {
        if (incident == null) return;

        // Condition: Severity is CRITICAL or Priority Score exceeds 200
        if (incident.getSeverity() == Severity.CRITICAL || incident.getPriority() >= 200.0) {
            // Check if already escalated for this incident to avoid spamming
            for (EscalationLog log : escalationLogs) {
                if (log.getIncidentId().equals(incident.getId())) {
                    return;
                }
            }

            String id = "ESC-999-" + String.format("%04d", escalationCounter++);
            EscalationLog log = new EscalationLog(
                    id,
                    incident.getId(),
                    incident.getDisasterType(),
                    incident.getLocation(),
                    incident.getPriority()
            );

            escalationLogs.add(log);
            System.out.println("🚨 [NATIONAL 999 SIMULATION] " + log);
        }
    }

    public synchronized List<EscalationLog> getEscalationLogs() {
        return Collections.unmodifiableList(new ArrayList<>(escalationLogs));
    }

    public synchronized void addSimulatedLog(EscalationLog log) {
        if (log != null) escalationLogs.add(log);
    }
}
