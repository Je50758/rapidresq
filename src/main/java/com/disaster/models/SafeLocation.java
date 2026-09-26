package com.disaster.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A shelter or safe area (cyclone/flood shelter, school, mosque, open field...)
 * with coordinates and capacity. Used to populate the public post-disaster
 * community report for the affected area.
 */
@Entity
@Table(name = "safe_locations")
public class SafeLocation {
    public enum LocationType {
        SHELTER("🏠 Cyclone/Flood Shelter"),
        SCHOOL("🏫 School Building"),
        MOSQUE("🕌 Mosque / Prayer Hall"),
        OPEN_FIELD("🏞️ Open Elevated Field"),
        HOSPITAL("🏥 Hospital"),
        COMMUNITY_CENTER("🏘️ Community Center");

        private final String label;
        LocationType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    @Id
    private String locationId;

    private String name;
    private String address;

    @Enumerated(EnumType.STRING)
    private LocationType type;

    private double latitude;
    private double longitude;

    private int capacityPersons;
    private String contactNumber;
    private boolean hasWaterSupply;
    private boolean hasMedicalPoint;
    private boolean active;

    private LocalDateTime createdTimestamp;

    public SafeLocation() {
        this.createdTimestamp = LocalDateTime.now();
        this.active = true;
    }

    public SafeLocation(String locationId, String name, String address, LocationType type,
                        double latitude, double longitude, int capacityPersons,
                        String contactNumber, boolean hasWaterSupply, boolean hasMedicalPoint) {
        this.locationId = locationId;
        this.name = name;
        this.address = address;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacityPersons = capacityPersons;
        this.contactNumber = contactNumber;
        this.hasWaterSupply = hasWaterSupply;
        this.hasMedicalPoint = hasMedicalPoint;
        this.active = true;
        this.createdTimestamp = LocalDateTime.now();
    }

    public double distanceKmTo(double lat, double lon) {
        double dLat = Math.toRadians(lat - latitude);
        double dLon = Math.toRadians(lon - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // --- Getters & Setters ---
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocationType getType() { return type; }
    public void setType(LocationType type) { this.type = type; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getCapacityPersons() { return capacityPersons; }
    public void setCapacityPersons(int capacityPersons) { this.capacityPersons = capacityPersons; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public boolean isHasWaterSupply() { return hasWaterSupply; }
    public void setHasWaterSupply(boolean hasWaterSupply) { this.hasWaterSupply = hasWaterSupply; }

    public boolean isHasMedicalPoint() { return hasMedicalPoint; }
    public void setHasMedicalPoint(boolean hasMedicalPoint) { this.hasMedicalPoint = hasMedicalPoint; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) { this.createdTimestamp = createdTimestamp; }
}
