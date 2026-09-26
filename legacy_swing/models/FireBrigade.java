package models;

/**
 * Specialized response unit for fire suppression, chemical hazard containment,
 * and high-rise thermal rescue.
 */
public class FireBrigade extends ResponseTeam {
    private int ladderReachMeters;

    public FireBrigade(String id, String name, String contactNumber, String baseStation, int ladderReachMeters) {
        super(id, name, contactNumber, baseStation);
        this.ladderReachMeters = Math.max(10, ladderReachMeters);
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        // FireBrigade primary focus is Fire, but can assist in structural collapse with gas hazard
        return incident.getDisasterType() == DisasterType.FIRE;
    }

    @Override
    public String getSpecialization() {
        return "Fire Suppression & Chemical Control (Ladder: " + ladderReachMeters + "m)";
    }

    public int getLadderReachMeters() {
        return ladderReachMeters;
    }

    public void setLadderReachMeters(int ladderReachMeters) {
        this.ladderReachMeters = Math.max(10, ladderReachMeters);
    }
}
