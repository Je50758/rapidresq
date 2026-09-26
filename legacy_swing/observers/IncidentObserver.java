package observers;

import models.Incident;
import models.ResponseTeam;

/**
 * Observer interface in the Observer Design Pattern.
 * Receives notifications of incident lifecycle events.
 */
public interface IncidentObserver {
    void onIncidentReported(Incident incident);
    void onIncidentVerified(Incident incident);
    void onTeamAssigned(Incident incident, ResponseTeam team);
    void onIncidentResolved(Incident incident);
}
