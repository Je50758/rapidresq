package com.disaster.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Donation request with donor contact details. PII (name/phone/address) is
 * encrypted at rest by the service layer and never returned publicly.
 */
public class DonationDto {
    // Optional: the nested route /needs/{needId}/contribute supplies it from the path.
    // Controllers validate presence when no path variable provides it.
    private String reliefNeedId;

    @NotNull(message = "Donation amount is required.")
    @DecimalMin(value = "0.01", message = "Donation amount must be greater than zero.")
    private Double amountBDT;

    private String reliefItem;

    @NotBlank(message = "Donor name is required.")
    @Size(max = 120, message = "Name must not exceed 120 characters.")
    private String donorName;

    /** Optional contact fields - collected for the admin donor registry, encrypted at rest. */
    @Size(max = 30, message = "Phone must not exceed 30 characters.")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Phone may only contain digits, spaces and + - ( )")
    private String donorPhone;

    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String donorAddress;

    /** Simulated payment channel (bKash / Nagad / Card). */
    private String paymentChannel;

    private boolean anonymous;

    public DonationDto() {
    }

    public DonationDto(String reliefNeedId, double amountBDT, String reliefItem, String donorUsername) {
        this.reliefNeedId = reliefNeedId;
        this.amountBDT = amountBDT;
        this.reliefItem = reliefItem;
        this.donorName = donorUsername;
    }

    public String getReliefNeedId() {
        return reliefNeedId;
    }

    public void setReliefNeedId(String reliefNeedId) {
        this.reliefNeedId = reliefNeedId;
    }

    public Double getAmountBDT() {
        return amountBDT;
    }

    public void setAmountBDT(Double amountBDT) {
        this.amountBDT = amountBDT;
    }

    public String getReliefItem() {
        return reliefItem;
    }

    public void setReliefItem(String reliefItem) {
        this.reliefItem = reliefItem;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getDonorAddress() {
        return donorAddress;
    }

    public void setDonorAddress(String donorAddress) {
        this.donorAddress = donorAddress;
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
}
