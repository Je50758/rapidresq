package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract class representing a specialized response unit.
 * Holds resources via Composition and defines polymorphic capability handling.
 */
public abstract class ResponseTeam {
    private final String id;
    private String name;
    private String contactNumber;
    private String baseStation;
    private TeamStatus status;
    private String currentIncidentId;
    private final List<Resource> resources; // Composition

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
     * Polymorphic method to determine whether this team has the expertise
     * and equipment to handle the given incident.
     */
    public abstract boolean canHandle(Incident incident);

    /**
     * Returns team specialization description.
     */
    public abstract String getSpecialization();

    /**
     * Assign team to an incident if currently available.
     */
    public boolean assignToIncident(String incidentId) {
        if (this.status == TeamStatus.AVAILABLE) {
            this.status = TeamStatus.ASSIGNED;
            this.currentIncidentId = incidentId;
            return true;
        }
        return false;
    }

    /**
     * Release team upon incident resolution.
     */
    public void release() {
        this.status = TeamStatus.AVAILABLE;
        this.currentIncidentId = null;
    }

    /**
     * Composition management: add resource to team.
     */
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

    // --- Getters & Setters ---

    public String getId() {
        return id;
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

    public boolean isAvailable() {
        return this.status == TeamStatus.AVAILABLE;
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
                id, name, getSpecialization(), status.name(), resources.size());
    }
}
