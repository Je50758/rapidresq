package com.disaster.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Specialized rapid response healthcare unit for triage, on-site trauma stabilization,
 * and critical casualty transit.
 */
@Entity
@DiscriminatorValue("MEDICAL_TEAM")
public class MedicalTeam extends ResponseTeam {
    private int paramedicCount;
    private boolean intensiveCareUnitEquipped;

    public MedicalTeam() {
        super();
    }

    public MedicalTeam(String id, String name, String contactNumber, String baseStation,
                       int paramedicCount, boolean intensiveCareUnitEquipped) {
        super(id, name, contactNumber, baseStation);
        this.paramedicCount = Math.max(1, paramedicCount);
        this.intensiveCareUnitEquipped = intensiveCareUnitEquipped;
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        return incident.getDisasterType() == DisasterType.ACCIDENT || incident.getInjuredCount() > 0;
    }

    @Override
    public String getSpecialization() {
        return "Trauma Care & Triage (Paramedics: " + paramedicCount +
                (intensiveCareUnitEquipped ? ", ICU Ready" : "") + ")";
    }

    public int getParamedicCount() {
        return paramedicCount;
    }

    public void setParamedicCount(int paramedicCount) {
        this.paramedicCount = Math.max(1, paramedicCount);
    }

    public boolean isIntensiveCareUnitEquipped() {
        return intensiveCareUnitEquipped;
    }

    public void setIntensiveCareUnitEquipped(boolean intensiveCareUnitEquipped) {
        this.intensiveCareUnitEquipped = intensiveCareUnitEquipped;
    }
}
