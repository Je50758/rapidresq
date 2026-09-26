package com.disaster.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;

/**
 * Represents a registered system user with role-based access and crowdsource trust scoring.
 * Passwords are hashed with BCrypt (salted &amp; deliberately slow) instead of raw digests.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;

    @JsonIgnore // Never serialize credential material in API responses
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** For volunteers: the structured team type they joined (null for other roles). */
    @Enumerated(EnumType.STRING)
    private VolunteerTeamType volunteerTeamType;

    private int trustScore; // 0 - 100
    private boolean active;

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public User() {
        this.active = true;
        this.trustScore = 50;
    }

    public User(String username, String rawPassword, Role role, int trustScore) {
        this.username = username != null ? username.trim().toLowerCase() : "";
        this.passwordHash = hashPassword(rawPassword);
        this.role = role != null ? role : Role.CITIZEN;
        this.trustScore = Math.max(0, Math.min(100, trustScore));
        this.active = true;
    }

    public static String hashPassword(String password) {
        return PASSWORD_ENCODER.encode(password == null ? "" : password);
    }

    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null) return false;
        // BCryptPasswordEncoder::matches is constant-time; guard against corrupt/empty hashes
        return passwordHash != null && !passwordHash.isEmpty()
                && PASSWORD_ENCODER.matches(rawPassword, passwordHash);
    }

    public void adjustTrustScore(int delta) {
        this.trustScore = Math.max(0, Math.min(100, this.trustScore + delta));
    }

    // --- Getters & Setters ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : "";
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPassword(String rawPassword) {
        this.passwordHash = hashPassword(rawPassword);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.CITIZEN;
    }

    public VolunteerTeamType getVolunteerTeamType() {
        return volunteerTeamType;
    }

    public void setVolunteerTeamType(VolunteerTeamType volunteerTeamType) {
        this.volunteerTeamType = volunteerTeamType;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = Math.max(0, Math.min(100, trustScore));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, Trust: %d, Active: %s)", username, role, trustScore, active);
    }
}
