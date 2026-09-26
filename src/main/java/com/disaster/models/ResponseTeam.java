package com.disaster.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class representing a specialized response unit.
 * Holds resources via Composition and defines polymorphic capability handling.
 */
@Entity
@Table(name = "response_teams")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "team_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ResponseTeam {
    @Id
    private String id;
    private String name;
    private String contactNumber;
    private String baseStation;

    @Enumerated(EnumType.STRING)
    private TeamStatus status;

    private String currentIncidentId;

    // --- Team operations & management fields (admin blueprint) ---
    private String leaderName;
    private Integer memberCount;

    /** Lat/Long home coordinates for proximity filtering & GPS mapping. */
    private double baseLatitude;
    private double baseLongitude;

    /** Last known live position (updated by unit check-ins / GPS pings). */
    private double lastLatitude;
    private double lastLongitude;

    /** Comma-separated skill tags, e.g. "swift-water, first-aid, drone-op". */
    @Column(length = 500)
    private String skillTags;

    /** When true the unit is locked to its current mission (Mission Lock Mode). */
    private boolean missionLocked;

    /** If set, the unit raises an alarm when GPS leaves this radius (meters) from mission location. */
    private Integer geofenceRadiusMeters;

    // Composition: ResponseTeam owns and manages physical resources
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id")
    private List<Resource> resources = new ArrayList<>();

    public ResponseTeam() {
        this.status = TeamStatus.AVAILABLE;
    }

    public ResponseTeam(String id, String name, String contactNumber, String baseStation) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.baseStation = baseStation;
        this.status = TeamStatus.AVAILABLE;
        this.currentIncidentId = null;
        this.resources = new ArrayList<>();
    }

    /**
     * Polymorphic method to determine whether this team can handle the given incident.
     */
    public abstract boolean canHandle(Incident incident);

    /**
     * Returns team specialization description.
     */
    public abstract String getSpecialization();

    public boolean assignToIncident(String incidentId) {
        if (this.status == TeamStatus.AVAILABLE) {
            this.status = TeamStatus.ASSIGNED;
            this.currentIncidentId = incidentId;
            return true;
        }
        return false;
    }

    public void release() {
        this.status = TeamStatus.AVAILABLE;
        this.currentIncidentId = null;
    }

    public void addResource(Resource resource) {
        if (resource != null && !resources.contains(resource)) {
            resources.add(resource);
        }
    }

    public void removeResource(Resource resource) {
        resources.remove(resource);
    }

    public List<Resource> getResources() {
        return Collections.unmodifiableList(resources);
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources != null ? resources : new ArrayList<>();
    }

    // --- Getters & Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getBaseStation() {
        return baseStation;
    }

    public void setBaseStation(String baseStation) {
        this.baseStation = baseStation;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public String getCurrentIncidentId() {
        return currentIncidentId;
    }

    public void setCurrentIncidentId(String currentIncidentId) {
        this.currentIncidentId = currentIncidentId;
    }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public double getBaseLatitude() { return baseLatitude; }
    public void setBaseLatitude(double baseLatitude) { this.baseLatitude = baseLatitude; }

    public double getBaseLongitude() { return baseLongitude; }
    public void setBaseLongitude(double baseLongitude) { this.baseLongitude = baseLongitude; }

    public double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(double lastLatitude) { this.lastLatitude = lastLatitude; }

    public double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(double lastLongitude) { this.lastLongitude = lastLongitude; }

    public String getSkillTags() { return skillTags; }
    public void setSkillTags(String skillTags) { this.skillTags = skillTags; }

    public boolean isMissionLocked() { return missionLocked; }
    public void setMissionLocked(boolean missionLocked) { this.missionLocked = missionLocked; }

    public Integer getGeofenceRadiusMeters() { return geofenceRadiusMeters; }
    public void setGeofenceRadiusMeters(Integer geofenceRadiusMeters) { this.geofenceRadiusMeters = geofenceRadiusMeters; }

    public boolean isAvailable() {
        return this.status == TeamStatus.AVAILABLE && !this.missionLocked;
    }

    /** Raw availability ignoring mission lock (used for admin views). */
    public boolean isStatusAvailable() {
        return this.status == TeamStatus.AVAILABLE;
    }

    /** Approximate distance in km from this team's last known position to the given point. */
    public double distanceKmTo(double lat, double lon) {
        double dLat = Math.toRadians(lat - lastLatitude);
        double dLon = Math.toRadians(lon - lastLongitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lastLatitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseTeam)) return false;
        ResponseTeam that = (ResponseTeam) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Status: %s | Resources: %d",
                id, name, getSpecialization(), status != null ? status.name() : "N/A", resources.size());
    }
}
