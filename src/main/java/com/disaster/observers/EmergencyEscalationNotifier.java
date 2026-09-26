package com.disaster.observers;

import com.disaster.models.EscalationLog;
import com.disaster.models.Incident;
import com.disaster.models.ResponseTeam;
import com.disaster.models.Severity;
import com.disaster.repository.EscalationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete Observer that simulates emergency escalation to the
 * National Emergency Service (999) whenever a CRITICAL severity incident is lodged.
 * Failures are logged, never swallowed silently.
 */
@Component
public class EmergencyEscalationNotifier implements IncidentObserver {
    private static final Logger log = LoggerFactory.getLogger(EmergencyEscalationNotifier.class);

    private final EscalationLogRepository escalationLogRepository;
    private final List<EscalationLog> inMemoryLogs = new ArrayList<>();
    private int escalationCounter = 1;

    @Autowired
    public EmergencyEscalationNotifier(EscalationLogRepository escalationLogRepository) {
        this.escalationLogRepository = escalationLogRepository;
    }

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
    }

    @Override
    public void onIncidentResolved(Incident incident) {
    }

    private synchronized void checkAndEscalate(Incident incident, String triggerReason) {
        if (incident == null) return;

        if (incident.getSeverity() == Severity.CRITICAL || incident.getPriority() >= 200.0) {
            for (EscalationLog log : inMemoryLogs) {
                if (log.getIncidentId().equals(incident.getId())) {
                    return;
                }
            }

            String id = "ESC-999-" + String.format("%04d", escalationCounter++);
            EscalationLog entry = new EscalationLog(
                    id,
                    incident.getId(),
                    incident.getDisasterType(),
                    incident.getLocation(),
                    incident.getPriority()
            );

            inMemoryLogs.add(entry);
            if (escalationLogRepository != null) {
                try {
                    escalationLogRepository.save(entry);
                } catch (Exception e) {
                    // In-memory copy still holds the record; surface the persistence failure
                    log.error("Failed to persist escalation log {} for incident {}: {}",
                            id, incident.getId(), e.getMessage());
                }
            }
            log.info("🚨 [NATIONAL 999 SIMULATION] {}", entry);
        }
    }

    public synchronized List<EscalationLog> getEscalationLogs() {
        if (escalationLogRepository != null) {
            List<EscalationLog> dbLogs = escalationLogRepository.findAllByOrderByTimestampDesc();
            if (!dbLogs.isEmpty()) return dbLogs;
        }
        return Collections.unmodifiableList(new ArrayList<>(inMemoryLogs));
    }

    public synchronized void addSimulatedLog(EscalationLog log) {
        if (log != null) {
            inMemoryLogs.add(log);
            if (escalationLogRepository != null) {
                escalationLogRepository.save(log);
            }
        }
    }
}
