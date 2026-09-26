package com.disaster.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Specialized response unit for fire suppression, chemical hazard containment,
 * and high-rise thermal rescue.
 */
@Entity
@DiscriminatorValue("FIRE_BRIGADE")
public class FireBrigade extends ResponseTeam {
    private int ladderReachMeters;

    public FireBrigade() {
        super();
    }

    public FireBrigade(String id, String name, String contactNumber, String baseStation, int ladderReachMeters) {
        super(id, name, contactNumber, baseStation);
        this.ladderReachMeters = Math.max(10, ladderReachMeters);
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
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
