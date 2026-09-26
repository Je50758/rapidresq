package com.disaster.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Record of citizen or corporate contributions to post-disaster rehabilitation.
 * <p>
 * Donor contact details (name, phone, address) are treated as sensitive PII:
 * they are AES-256-GCM encrypted at rest by the service layer and are NEVER
 * serialized to public API responses. JSON exposes only a masked display name
 * (e.g. "Farhan K."). The full decrypted view is produced exclusively for
 * administrators via {@link #toAdminJson(EncryptionView)}.
 */
@Entity
@Table(name = "donation_records")
public class DonationRecord {
    @Id
    private String donationId;
    private String reliefNeedId;

    /** Platform account username of the donor (or "anonymous_donor"). Not sensitive. */
    private String donorUsername;

    /**
     * Public display name. Masked on serialization; the raw value is also stored
     * encrypted in donorNameEnc for the admin registry.
     */
    private String donorName;

    // --- Sensitive PII: stored ENCRYPTED, never serialized publicly ---
    @JsonIgnore
    private String donorPhoneEnc;

    @JsonIgnore
    private String donorAddressEnc;

    private double amountBDT;
    private String reliefItem;
    private String paymentChannel;
    private boolean anonymous;
    private LocalDateTime timestamp;

    public DonationRecord() {
        this.timestamp = LocalDateTime.now();
    }

    public DonationRecord(String donationId, String reliefNeedId, String donorUsername, double amountBDT, String reliefItem) {
        this.donationId = donationId;
        this.reliefNeedId = reliefNeedId;
        this.donorUsername = donorUsername != null ? donorUsername : "anonymous_donor";
        this.amountBDT = Math.max(0.0, amountBDT);
        this.reliefItem = reliefItem != null ? reliefItem : "General Relief Fund";
        this.timestamp = LocalDateTime.now();
    }

    /** Masks a donor name for public display: "Farhan Kaif" -> "Farhan K.", single-word -> first 2 chars. */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return "Anonymous";
        String trimmed = name.trim();
        if ("anonymous".equalsIgnoreCase(trimmed) || "anonymous citizen".equalsIgnoreCase(trimmed)) {
            return "Anonymous";
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1].charAt(0) + ".";
        }
        return trimmed.length() <= 2 ? trimmed : trimmed.substring(0, 2) + ".";
    }

    /** Public JSON: donor identity masked, PII omitted entirely. */
    public String getDonorName() {
        return maskName(donorName);
    }

    /** Raw (unmasked) donor name - used by the admin registry view only. */
    @JsonIgnore
    public String getRawDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    @JsonIgnore
    public String getDonorPhoneEnc() {
        return donorPhoneEnc;
    }

    public void setDonorPhoneEnc(String donorPhoneEnc) {
        this.donorPhoneEnc = donorPhoneEnc;
    }

    @JsonIgnore
    public String getDonorAddressEnc() {
        return donorAddressEnc;
    }

    public void setDonorAddressEnc(String donorAddressEnc) {
        this.donorAddressEnc = donorAddressEnc;
    }

    public String getDonorUsername() {
        return donorUsername;
    }

    public void setDonorUsername(String donorUsername) {
        this.donorUsername = donorUsername;
    }

    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }

    public String getReliefNeedId() {
        return reliefNeedId;
    }

    public void setReliefNeedId(String reliefNeedId) {
        this.reliefNeedId = reliefNeedId;
    }

    public double getAmountBDT() {
        return amountBDT;
    }

    public void setAmountBDT(double amountBDT) {
        this.amountBDT = amountBDT;
    }

    public String getReliefItem() {
        return reliefItem;
    }

    public void setReliefItem(String reliefItem) {
        this.reliefItem = reliefItem;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    /**
     * Admin-only full view with decrypted PII. Never returned from public endpoints.
     */
    public Map<String, Object> toAdminJson(String decryptedPhone, String decryptedAddress) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("donationId", donationId);
        view.put("reliefNeedId", reliefNeedId);
        view.put("donorUsername", donorUsername);
        view.put("donorName", donorName != null ? donorName : donorUsername);
        view.put("donorPhone", decryptedPhone);
        view.put("donorAddress", decryptedAddress);
        view.put("amountBDT", amountBDT);
        view.put("reliefItem", reliefItem);
        view.put("paymentChannel", paymentChannel);
        view.put("anonymous", anonymous);
        view.put("formattedTimestamp", getFormattedTimestamp());
        return view;
    }

    @Override
    public String toString() {
        return String.format("[%s] ৳%.2f (%s) by @%s at %s",
                donationId, amountBDT, reliefItem, donorUsername, getFormattedTimestamp());
    }
}
