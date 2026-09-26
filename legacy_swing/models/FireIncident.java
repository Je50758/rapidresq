package models;

/**
 * Concrete incident representing a fire outbreak.
 * Polymorphically calculates priority based on alarm level, chemical hazard, and injuries.
 */
public class FireIncident extends Incident {
    private int fireAlarmLevel; // 1 to 5
    private boolean chemicalOrGasHazard;

    public FireIncident(String id, String location, int injuredCount, Severity severity,
                        String reporterUsername, int reporterTrustScore, String description,
                        int fireAlarmLevel, boolean chemicalOrGasHazard) {
        super(id, location, injuredCount, severity, reporterUsername, reporterTrustScore, description);
        this.fireAlarmLevel = Math.max(1, Math.min(5, fireAlarmLevel));
        this.chemicalOrGasHazard = chemicalOrGasHazard;
        // Re-evaluate priority after specific attributes set
        setPriority(calculatePriority());
    }

    @Override
    public double calculatePriority() {
        double base = getSeverity() != null ? getSeverity().getWeight() * 25.0 : 25.0;
        double injuryWeight = getInjuredCount() * 12.0;
        double alarmWeight = fireAlarmLevel * 8.0;
        double hazardWeight = chemicalOrGasHazard ? 30.0 : 0.0;
        double crowdConfidence = getReportCount() * 3.0;

        return Math.round((base + injuryWeight + alarmWeight + hazardWeight + crowdConfidence) * 10.0) / 10.0;
    }

    @Override
    public DisasterType getDisasterType() {
        return DisasterType.FIRE;
    }

    public int getFireAlarmLevel() {
        return fireAlarmLevel;
    }

    public void setFireAlarmLevel(int fireAlarmLevel) {
        this.fireAlarmLevel = Math.max(1, Math.min(5, fireAlarmLevel));
        setPriority(calculatePriority());
    }

    public boolean isChemicalOrGasHazard() {
        return chemicalOrGasHazard;
    }

    public void setChemicalOrGasHazard(boolean chemicalOrGasHazard) {
        this.chemicalOrGasHazard = chemicalOrGasHazard;
        setPriority(calculatePriority());
    }
}
