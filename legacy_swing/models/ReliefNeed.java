package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a post-disaster community rehabilitation need generated
 * automatically upon resolving an emergency incident.
 */
public class ReliefNeed {
    public enum NeedStatus {
        ACTIVE,
        FULFILLED
    }

    private final String needId;
    private final String incidentId;
    private final String location;
    private final DisasterType disasterType;
    private final String description;
    private final double targetFundingBDT;
    private double collectedFundingBDT;
    private final String primaryReliefPackage;
    private final int targetPackagesCount;
    private int collectedPackagesCount;
    private NeedStatus status;
    private final LocalDateTime createdTimestamp;
    private final List<DonationRecord> donations;

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

    /**
     * Registers a donation contribution towards this relief drive.
     */
    public synchronized void recordDonation(DonationRecord donation) {
        if (donation == null) return;
        donations.add(donation);
        this.collectedFundingBDT += donation.getAmountBDT();

        // If package donated, increment package count
        if (donation.getReliefItem() != null && !donation.getReliefItem().equalsIgnoreCase("Cash Aid")) {
            this.collectedPackagesCount++;
        }

        // Auto-fulfill when both funding and packages reach target
        if (this.collectedFundingBDT >= this.targetFundingBDT && this.collectedPackagesCount >= this.targetPackagesCount) {
            this.status = NeedStatus.FULFILLED;
        }
    }

    public double getFundingProgressPercent() {
        if (targetFundingBDT <= 0) return 100.0;
        return Math.min(100.0, (collectedFundingBDT / targetFundingBDT) * 100.0);
    }

    // --- Getters ---

    public String getNeedId() {
        return needId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getLocation() {
        return location;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public String getDescription() {
        return description;
    }

    public double getTargetFundingBDT() {
        return targetFundingBDT;
    }

    public double getCollectedFundingBDT() {
        return collectedFundingBDT;
    }

    public String getPrimaryReliefPackage() {
        return primaryReliefPackage;
    }

    public int getTargetPackagesCount() {
        return targetPackagesCount;
    }

    public int getCollectedPackagesCount() {
        return collectedPackagesCount;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getFormattedCreatedTimestamp() {
        return createdTimestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public List<DonationRecord> getDonations() {
        return Collections.unmodifiableList(donations);
    }

    @Override
    public String toString() {
        return String.format("[%s] Relief for %s (%s) | Goal: ৳%.0f (Raised: ৳%.0f - %.1f%%) | Status: %s",
                needId, location, disasterType, targetFundingBDT, collectedFundingBDT, getFundingProgressPercent(), status);
    }
}
