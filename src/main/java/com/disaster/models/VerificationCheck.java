package com.disaster.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The result of one admin-run verification check (one of the {@link VerificationMethod}s)
 * against one incident. Successful checks accumulate; enough weight auto-verifies the incident.
 */
@Entity
@Table(name = "verification_checks")
public class VerificationCheck {
    @Id
    private String checkId;

    @Enumerated(EnumType.STRING)
    private VerificationMethod method;

    private String incidentId;

    /** true = check passed (confirms the report), false = flagged as suspicious. */
    private boolean passed;

    @Column(length = 1000)
    private String notes;

    private String checkedByAdmin;

    private LocalDateTime checkedAt;

    public VerificationCheck() {
        this.checkedAt = LocalDateTime.now();
    }

    public VerificationCheck(String checkId, VerificationMethod method, String incidentId,
                             boolean passed, String notes, String checkedByAdmin) {
        this.checkId = checkId;
        this.method = method;
        this.incidentId = incidentId;
        this.passed = passed;
        this.notes = notes;
        this.checkedByAdmin = checkedByAdmin;
        this.checkedAt = LocalDateTime.now();
    }

    public String getFormattedCheckedAt() {
        return checkedAt != null ? checkedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
    }

    // --- Getters & Setters ---
    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }

    public VerificationMethod getMethod() { return method; }
    public void setMethod(VerificationMethod method) { this.method = method; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCheckedByAdmin() { return checkedByAdmin; }
    public void setCheckedByAdmin(String checkedByAdmin) { this.checkedByAdmin = checkedByAdmin; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
}
