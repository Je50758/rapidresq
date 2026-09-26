package models;

/**
 * Severity level of an incident, determined by rule-based evaluation.
 */
public enum Severity {
    LOW(1.0, "Low Risk"),
    MODERATE(1.8, "Moderate Threat"),
    CRITICAL(2.8, "Critical Emergency");

    private final double weight;
    private final String description;

    Severity(double weight, String description) {
        this.weight = weight;
        this.description = description;
    }

    public double getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}
