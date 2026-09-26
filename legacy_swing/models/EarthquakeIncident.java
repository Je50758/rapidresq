package models;

/**
 * Concrete incident representing earthquake tremors and structural collapses.
 * Polymorphically calculates priority based on Richter magnitude and structural collapses.
 */
public class EarthquakeIncident extends Incident {
    private double magnitudeRichter;
    private int collapsedBuildingsCount;

    public EarthquakeIncident(String id, String location, int injuredCount, Severity severity,
                              String reporterUsername, int reporterTrustScore, String description,
                              double magnitudeRichter, int collapsedBuildingsCount) {
        super(id, location, injuredCount, severity, reporterUsername, reporterTrustScore, description);
        this.magnitudeRichter = Math.max(1.0, magnitudeRichter);
        this.collapsedBuildingsCount = Math.max(0, collapsedBuildingsCount);
        setPriority(calculatePriority());
    }

    @Override
    public double calculatePriority() {
        double base = getSeverity() != null ? getSeverity().getWeight() * 25.0 : 25.0;
        double magnitudeWeight = magnitudeRichter * 12.0;
        double collapseWeight = collapsedBuildingsCount * 22.0;
        double injuryWeight = getInjuredCount() * 15.0;
        double crowdConfidence = getReportCount() * 3.0;

        return Math.round((base + magnitudeWeight + collapseWeight + injuryWeight + crowdConfidence) * 10.0) / 10.0;
    }

    @Override
    public DisasterType getDisasterType() {
        return DisasterType.EARTHQUAKE;
    }

    public double getMagnitudeRichter() {
        return magnitudeRichter;
    }

    public void setMagnitudeRichter(double magnitudeRichter) {
        this.magnitudeRichter = Math.max(1.0, magnitudeRichter);
        setPriority(calculatePriority());
    }

    public int getCollapsedBuildingsCount() {
        return collapsedBuildingsCount;
    }

    public void setCollapsedBuildingsCount(int collapsedBuildingsCount) {
        this.collapsedBuildingsCount = Math.max(0, collapsedBuildingsCount);
        setPriority(calculatePriority());
    }
}
