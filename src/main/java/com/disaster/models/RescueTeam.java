package com.disaster.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Specialized search and rescue unit trained for urban structural collapses,
 * earthquake debris extraction, and deep-water flood rescues.
 */
@Entity
@DiscriminatorValue("RESCUE_TEAM")
public class RescueTeam extends ResponseTeam {
    private boolean waterRescueCertified;
    private boolean heavyDebrisExtractionCertified;

    public RescueTeam() {
        super();
    }

    public RescueTeam(String id, String name, String contactNumber, String baseStation,
                      boolean waterRescueCertified, boolean heavyDebrisExtractionCertified) {
        super(id, name, contactNumber, baseStation);
        this.waterRescueCertified = waterRescueCertified;
        this.heavyDebrisExtractionCertified = heavyDebrisExtractionCertified;
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        if (incident.getDisasterType() == DisasterType.EARTHQUAKE && heavyDebrisExtractionCertified) {
            return true;
        }
        if (incident.getDisasterType() == DisasterType.FLOOD && waterRescueCertified) {
            return true;
        }
        return incident.getDisasterType() == DisasterType.EARTHQUAKE || incident.getDisasterType() == DisasterType.FLOOD;
    }

    @Override
    public String getSpecialization() {
        return "Search & Rescue (" +
                (waterRescueCertified ? "Water Ops " : "") +
                (heavyDebrisExtractionCertified ? "Urban Debris Extraction" : "") + ")";
    }

    public boolean isWaterRescueCertified() {
        return waterRescueCertified;
    }

    public void setWaterRescueCertified(boolean waterRescueCertified) {
        this.waterRescueCertified = waterRescueCertified;
    }

    public boolean isHeavyDebrisExtractionCertified() {
        return heavyDebrisExtractionCertified;
    }

    public void setHeavyDebrisExtractionCertified(boolean heavyDebrisExtractionCertified) {
        this.heavyDebrisExtractionCertified = heavyDebrisExtractionCertified;
    }
}
