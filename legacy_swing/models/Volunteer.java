package models;

/**
 * Community-driven first responder volunteer unit.
 * Provides rapid local relief, preliminary verification, and evacuation aid
 * before heavy national agencies arrive.
 */
public class Volunteer extends ResponseTeam {
    private int volunteerMemberCount;
    private String communityDistrict;

    public Volunteer(String id, String name, String contactNumber, String baseStation,
                     int volunteerMemberCount, String communityDistrict) {
        super(id, name, contactNumber, baseStation);
        this.volunteerMemberCount = Math.max(1, volunteerMemberCount);
        this.communityDistrict = communityDistrict != null ? communityDistrict : "General Zone";
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        // Volunteers can act as first responders for Low and Moderate severity incidents across all types,
        // or support flood relief and road accident crowd control.
        return incident.getSeverity() == Severity.LOW || incident.getSeverity() == Severity.MODERATE;
    }

    @Override
    public String getSpecialization() {
        return "Community First Responders (Members: " + volunteerMemberCount + " | Area: " + communityDistrict + ")";
    }

    public int getVolunteerMemberCount() {
        return volunteerMemberCount;
    }

    public void setVolunteerMemberCount(int volunteerMemberCount) {
        this.volunteerMemberCount = Math.max(1, volunteerMemberCount);
    }

    public String getCommunityDistrict() {
        return communityDistrict;
    }

    public void setCommunityDistrict(String communityDistrict) {
        this.communityDistrict = communityDistrict;
    }
}
