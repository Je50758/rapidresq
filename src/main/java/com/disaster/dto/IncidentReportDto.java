package com.disaster.dto;

import com.disaster.models.DisasterType;
import com.disaster.models.Severity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentReportDto {
    @NotNull(message = "Disaster type is required.")
    private DisasterType disasterType;

    @NotBlank(message = "Location is required for emergency dispatch.")
    @Size(max = 255, message = "Location must not exceed 255 characters.")
    private String location;

    @Min(value = 0, message = "Injured count cannot be negative.")
    private int injuredCount;

    @NotNull(message = "Severity is required.")
    private Severity severity;

    @Size(max = 2000, message = "Description must not exceed 2000 characters.")
    private String description;

    // Fire specific
    private Integer fireAlarmLevel;
    private Boolean chemicalOrGasHazard;

    // Flood specific
    private Double waterLevelMeters;
    private Integer strandedPeopleCount;

    // Accident specific
    private Integer vehiclesInvolved;
    private Boolean highwayOrTrain;

    // Earthquake specific
    private Double magnitudeRichter;
    private Integer collapsedBuildingsCount;

    /** Optional report coordinates for shelter matching & proximity dispatch. */
    private Double latitude;
    private Double longitude;

    public IncidentReportDto() {
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getInjuredCount() {
        return injuredCount;
    }

    public void setInjuredCount(int injuredCount) {
        this.injuredCount = injuredCount;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFireAlarmLevel() {
        return fireAlarmLevel != null ? fireAlarmLevel : 1;
    }

    public void setFireAlarmLevel(Integer fireAlarmLevel) {
        this.fireAlarmLevel = fireAlarmLevel;
    }

    public Boolean getChemicalOrGasHazard() {
        return chemicalOrGasHazard != null && chemicalOrGasHazard;
    }

    public void setChemicalOrGasHazard(Boolean chemicalOrGasHazard) {
        this.chemicalOrGasHazard = chemicalOrGasHazard;
    }

    public Double getWaterLevelMeters() {
        return waterLevelMeters != null ? waterLevelMeters : 1.0;
    }

    public void setWaterLevelMeters(Double waterLevelMeters) {
        this.waterLevelMeters = waterLevelMeters;
    }

    public Integer getStrandedPeopleCount() {
        return strandedPeopleCount != null ? strandedPeopleCount : 0;
    }

    public void setStrandedPeopleCount(Integer strandedPeopleCount) {
        this.strandedPeopleCount = strandedPeopleCount;
    }

    public Integer getVehiclesInvolved() {
        return vehiclesInvolved != null ? vehiclesInvolved : 1;
    }

    public void setVehiclesInvolved(Integer vehiclesInvolved) {
        this.vehiclesInvolved = vehiclesInvolved;
    }

    public Boolean getHighwayOrTrain() {
        return highwayOrTrain != null && highwayOrTrain;
    }

    public void setHighwayOrTrain(Boolean highwayOrTrain) {
        this.highwayOrTrain = highwayOrTrain;
    }

    public Double getMagnitudeRichter() {
        return magnitudeRichter != null ? magnitudeRichter : 4.0;
    }

    public void setMagnitudeRichter(Double magnitudeRichter) {
        this.magnitudeRichter = magnitudeRichter;
    }

    public Integer getCollapsedBuildingsCount() {
        return collapsedBuildingsCount != null ? collapsedBuildingsCount : 0;
    }

    public void setCollapsedBuildingsCount(Integer collapsedBuildingsCount) {
        this.collapsedBuildingsCount = collapsedBuildingsCount;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
