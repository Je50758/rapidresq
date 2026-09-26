package com.disaster.dto;

import com.disaster.models.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters.")
    private String password;

    private Role role;

    /** Required when role == VOLUNTEER: which structured volunteer team to join. */
    private com.disaster.models.VolunteerTeamType volunteerTeamType;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public com.disaster.models.VolunteerTeamType getVolunteerTeamType() {
        return volunteerTeamType;
    }

    public void setVolunteerTeamType(com.disaster.models.VolunteerTeamType volunteerTeamType) {
        this.volunteerTeamType = volunteerTeamType;
    }
}
