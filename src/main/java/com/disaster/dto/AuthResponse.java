package com.disaster.dto;

import com.disaster.models.Role;

public class AuthResponse {
    private String token;
    private String username;
    private Role role;
    private int trustScore;
    private String message;

    public AuthResponse() {
    }

    public AuthResponse(String token, String username, Role role, int trustScore, String message) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.trustScore = trustScore;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = trustScore;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
