package auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Represents a system user with role-based access and crowdsource trust scoring.
 */
public class User {
    private final String username;
    private String passwordHash;
    private Role role;
    private int trustScore; // Default 50, range 0 - 100
    private boolean active;

    public User(String username, String rawPassword, Role role, int trustScore) {
        this.username = username.trim().toLowerCase();
        this.passwordHash = hashPassword(rawPassword);
        this.role = role != null ? role : Role.CITIZEN;
        this.trustScore = Math.max(0, Math.min(100, trustScore));
        this.active = true;
    }

    /**
     * Hashes plain text password using standard SHA-256 algorithm.
     */
    public static String hashPassword(String password) {
        if (password == null) password = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available in environment", e);
        }
    }

    /**
     * Validates candidate password against stored SHA-256 hash.
     */
    public boolean verifyPassword(String rawPassword) {
        return Objects.equals(this.passwordHash, hashPassword(rawPassword));
    }

    // --- Getters & Setters ---

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPassword(String newRawPassword) {
        this.passwordHash = hashPassword(newRawPassword);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.CITIZEN;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = Math.max(0, Math.min(100, trustScore));
    }

    public void adjustTrustScore(int delta) {
        this.trustScore = Math.max(0, Math.min(100, this.trustScore + delta));
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
