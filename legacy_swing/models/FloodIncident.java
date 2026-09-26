package models;

/**
 * Concrete incident representing floods and severe waterlogging.
 * Polymorphically calculates priority based on water depth and stranded citizens.
 */
public class FloodIncident extends Incident {
    private double waterLevelMeters;
    private int strandedPeopleCount;

    public FloodIncident(String id, String location, int injuredCount, Severity severity,
                         String reporterUsername, int reporterTrustScore, String description,
                         double waterLevelMeters, int strandedPeopleCount) {
        super(id, location, injuredCount, severity, reporterUsername, reporterTrustScore, description);
        this.waterLevelMeters = Math.max(0.0, waterLevelMeters);
        this.strandedPeopleCount = Math.max(0, strandedPeopleCount);
        setPriority(calculatePriority());
    }

    @Override
    public double calculatePriority() {
        double base = getSeverity() != null ? getSeverity().getWeight() * 20.0 : 20.0;
        double injuryWeight = getInjuredCount() * 10.0;
        double strandedWeight = strandedPeopleCount * 4.5;
        double waterWeight = waterLevelMeters * 8.0;
        double crowdConfidence = getReportCount() * 3.0;

        return Math.round((base + injuryWeight + strandedWeight + waterWeight + crowdConfidence) * 10.0) / 10.0;
    }

    @Override
    public DisasterType getDisasterType() {
        return DisasterType.FLOOD;
    }

    public double getWaterLevelMeters() {
        return waterLevelMeters;
    }

    public void setWaterLevelMeters(double waterLevelMeters) {
        this.waterLevelMeters = Math.max(0.0, waterLevelMeters);
        setPriority(calculatePriority());
    }

    public int getStrandedPeopleCount() {
        return strandedPeopleCount;
    }

    public void setStrandedPeopleCount(int strandedPeopleCount) {
        this.strandedPeopleCount = Math.max(0, strandedPeopleCount);
        setPriority(calculatePriority());
    }
}
