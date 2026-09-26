package com.disaster.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Concrete incident representing highway, road, or mass transport accidents.
 * Polymorphically calculates priority based on casualty risk and vehicle involvement.
 */
@Entity
@DiscriminatorValue("ACCIDENT")
public class AccidentIncident extends Incident {
    private int vehiclesInvolved;
    private boolean highwayOrTrain;

    public AccidentIncident() {
        super();
    }

    public AccidentIncident(String id, String location, int injuredCount, Severity severity,
                            String reporterUsername, int reporterTrustScore, String description,
                            int vehiclesInvolved, boolean highwayOrTrain) {
        super(id, location, injuredCount, severity, reporterUsername, reporterTrustScore, description);
        this.vehiclesInvolved = Math.max(1, vehiclesInvolved);
        this.highwayOrTrain = highwayOrTrain;
        setPriority(calculatePriority());
    }

    @Override
    public double calculatePriority() {
        double base = getSeverity() != null ? getSeverity().getWeight() * 20.0 : 20.0;
        double injuryWeight = getInjuredCount() * 15.0;
        double vehicleWeight = vehiclesInvolved * 6.0;
        double highwayWeight = highwayOrTrain ? 25.0 : 0.0;
        double crowdConfidence = getReportCount() * 3.0;

        return Math.round((base + injuryWeight + vehicleWeight + highwayWeight + crowdConfidence) * 10.0) / 10.0;
    }

    @Override
    public DisasterType getDisasterType() {
        return DisasterType.ACCIDENT;
    }

    public int getVehiclesInvolved() {
        return vehiclesInvolved;
    }

    public void setVehiclesInvolved(int vehiclesInvolved) {
        this.vehiclesInvolved = Math.max(1, vehiclesInvolved);
        setPriority(calculatePriority());
    }

    public boolean isHighwayOrTrain() {
        return highwayOrTrain;
    }

    public void setHighwayOrTrain(boolean highwayOrTrain) {
        this.highwayOrTrain = highwayOrTrain;
        setPriority(calculatePriority());
    }
}
