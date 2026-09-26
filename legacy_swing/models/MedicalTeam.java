package models;

/**
 * Specialized rapid response healthcare unit for triage, on-site trauma stabilization,
 * and critical casualty transit.
 */
public class MedicalTeam extends ResponseTeam {
    private int paramedicCount;
    private boolean intensiveCareUnitEquipped;

    public MedicalTeam(String id, String name, String contactNumber, String baseStation,
                       int paramedicCount, boolean intensiveCareUnitEquipped) {
        super(id, name, contactNumber, baseStation);
        this.paramedicCount = Math.max(1, paramedicCount);
        this.intensiveCareUnitEquipped = intensiveCareUnitEquipped;
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        // Medical team handles road accidents and any incident with reported injuries
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
