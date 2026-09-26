package com.disaster.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a post-disaster community rehabilitation need generated
 * automatically upon resolving an emergency incident.
 */
@Entity
@Table(name = "relief_needs")
public class ReliefNeed {
    public enum NeedStatus {
        ACTIVE,
        FULFILLED
    }

    @Id
    private String needId;
    private String incidentId;
    private String location;

    @Enumerated(EnumType.STRING)
    private DisasterType disasterType;

    @Column(length = 2000)
    private String description;

    private double targetFundingBDT;
    private double collectedFundingBDT;
    private String primaryReliefPackage;
    private int targetPackagesCount;
    private int collectedPackagesCount;

    @Enumerated(EnumType.STRING)
    private NeedStatus status;

    private LocalDateTime createdTimestamp;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "target_need_id")
    private List<DonationRecord> donations = new ArrayList<>();

    public ReliefNeed() {
        this.status = NeedStatus.ACTIVE;
        this.createdTimestamp = LocalDateTime.now();
    }

    public ReliefNeed(String needId, String incidentId, String location, DisasterType disasterType,
                      String description, double targetFundingBDT, String primaryReliefPackage, int targetPackagesCount) {
        this.needId = needId;
        this.incidentId = incidentId;
        this.location = location;
        this.disasterType = disasterType;
        this.description = description;
        this.targetFundingBDT = Math.max(1000.0, targetFundingBDT);
        this.collectedFundingBDT = 0.0;
        this.primaryReliefPackage = primaryReliefPackage != null ? primaryReliefPackage : "Emergency Ration Packs";
        this.targetPackagesCount = Math.max(10, targetPackagesCount);
        this.collectedPackagesCount = 0;
        this.status = NeedStatus.ACTIVE;
        this.createdTimestamp = LocalDateTime.now();
        this.donations = new ArrayList<>();
    }

    public synchronized void recordDonation(DonationRecord donation) {
        if (donation == null) return;
        donations.add(donation);
        this.collectedFundingBDT += donation.getAmountBDT();

        if (donation.getReliefItem() != null && !donation.getReliefItem().equalsIgnoreCase("Cash Aid")) {
            this.collectedPackagesCount++;
        }

        if (this.collectedFundingBDT >= this.targetFundingBDT && this.collectedPackagesCount >= this.targetPackagesCount) {
            this.status = NeedStatus.FULFILLED;
        }
    }

    public double getFundingProgressPercent() {
        if (targetFundingBDT <= 0) return 100.0;
        return Math.min(100.0, (collectedFundingBDT / targetFundingBDT) * 100.0);
    }

    // --- Getters & Setters ---

    public String getNeedId() {
        return needId;
    }

    public void setNeedId(String needId) {
        this.needId = needId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTargetFundingBDT() {
        return targetFundingBDT;
    }

    public void setTargetFundingBDT(double targetFundingBDT) {
        this.targetFundingBDT = targetFundingBDT;
    }

    public double getCollectedFundingBDT() {
        return collectedFundingBDT;
    }

    public void setCollectedFundingBDT(double collectedFundingBDT) {
        this.collectedFundingBDT = collectedFundingBDT;
    }

    public String getPrimaryReliefPackage() {
        return primaryReliefPackage;
    }

    public void setPrimaryReliefPackage(String primaryReliefPackage) {
        this.primaryReliefPackage = primaryReliefPackage;
    }

    public int getTargetPackagesCount() {
        return targetPackagesCount;
    }

    public void setTargetPackagesCount(int targetPackagesCount) {
        this.targetPackagesCount = targetPackagesCount;
    }

    public int getCollectedPackagesCount() {
        return collectedPackagesCount;
    }

    public void setCollectedPackagesCount(int collectedPackagesCount) {
        this.collectedPackagesCount = collectedPackagesCount;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public void setStatus(NeedStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getFormattedCreatedTimestamp() {
        return createdTimestamp != null ? createdTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    public List<DonationRecord> getDonations() {
        return Collections.unmodifiableList(donations);
    }

    public void setDonations(List<DonationRecord> donations) {
        this.donations = donations != null ? donations : new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("[%s] Relief for %s (%s) | Goal: ৳%.0f (Raised: ৳%.0f - %.1f%%) | Status: %s",
                needId, location, disasterType, targetFundingBDT, collectedFundingBDT, getFundingProgressPercent(), status);
    }
}
