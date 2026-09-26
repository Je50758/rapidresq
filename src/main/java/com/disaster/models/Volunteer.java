package com.disaster.models;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Community-driven first responder volunteer unit.
 * Provides rapid local relief, preliminary verification, and evacuation aid
 * before heavy national agencies arrive.
 */
@Entity
@DiscriminatorValue("VOLUNTEER")
public class Volunteer extends ResponseTeam {
    private int volunteerMemberCount;
    private String communityDistrict;

    /** Structured category this volunteer unit belongs to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "volunteer_team_type")
    private VolunteerTeamType teamType;

    public Volunteer() {
        super();
    }

    public Volunteer(String id, String name, String contactNumber, String baseStation,
                     int volunteerMemberCount, String communityDistrict) {
        super(id, name, contactNumber, baseStation);
        this.volunteerMemberCount = Math.max(1, volunteerMemberCount);
        this.communityDistrict = communityDistrict != null ? communityDistrict : "General Zone";
        this.teamType = VolunteerTeamType.RESCUE;
    }

    public VolunteerTeamType getTeamType() { return teamType; }
    public void setTeamType(VolunteerTeamType teamType) {
        this.teamType = teamType != null ? teamType : VolunteerTeamType.RESCUE;
    }

    @Override
    public boolean canHandle(Incident incident) {
        if (incident == null) return false;
        return incident.getSeverity() == Severity.LOW || incident.getSeverity() == Severity.MODERATE;
    }

    @Override
    public String getSpecialization() {
        String typeName = teamType != null ? teamType.getDisplayName() : "Generalist";
        return typeName + " (Members: " + volunteerMemberCount + " | Area: " + communityDistrict + ")";
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
