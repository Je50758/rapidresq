package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Record of citizen or corporate contributions to post-disaster rehabilitation.
 */
public class DonationRecord {
    private final String donationId;
    private final String reliefNeedId;
    private final String donorUsername;
    private final double amountBDT;
    private final String reliefItem;
    private final LocalDateTime timestamp;

    public DonationRecord(String donationId, String reliefNeedId, String donorUsername, double amountBDT, String reliefItem) {
        this.donationId = donationId;
        this.reliefNeedId = reliefNeedId;
        this.donorUsername = donorUsername != null ? donorUsername : "anonymous_donor";
        this.amountBDT = Math.max(0.0, amountBDT);
        this.reliefItem = reliefItem != null ? reliefItem : "General Relief Fund";
        this.timestamp = LocalDateTime.now();
    }

    public String getDonationId() {
        return donationId;
    }

    public String getReliefNeedId() {
        return reliefNeedId;
    }

    public String getDonorUsername() {
        return donorUsername;
    }

    public double getAmountBDT() {
        return amountBDT;
    }

    public String getReliefItem() {
        return reliefItem;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("[%s] ৳%.2f (%s) by @%s at %s",
                donationId, amountBDT, reliefItem, donorUsername, getFormattedTimestamp());
    }
}
