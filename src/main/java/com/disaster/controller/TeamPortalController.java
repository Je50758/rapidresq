package com.disaster.controller;

import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.NoTeamAvailableException;
import com.disaster.models.*;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import com.disaster.service.UserAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RESPONSE_TEAM program controller: this role manages the volunteer program -
 * recruitment & registration, team-type assignment, activation, volunteer
 * dispatch to disasters, and demobilization. Admin retains full command; the
 * RESPONSE_TEAM scope is deliberately limited to volunteer units.
 */
@RestController
@RequestMapping("/api/team-portal")
public class TeamPortalController {

    private final DisasterManagementService disasterService;
    private final UserAuthenticationService userAuthService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public TeamPortalController(DisasterManagementService disasterService,
                                UserAuthenticationService userAuthService,
                                TokenAuthService tokenAuthService) {
        this.disasterService = disasterService;
        this.userAuthService = userAuthService;
        this.tokenAuthService = tokenAuthService;
    }

    /** RESPONSE_TEAM or ADMIN required; returns the actor's username. */
    private String requireProgramManager(String authHeader) {
        tokenAuthService.requireRole(authHeader, Role.RESPONSE_TEAM, Role.ADMIN);
        return tokenAuthService.requireUser(authHeader).getUsername();
    }

    // =====================================================================
    // 1. Volunteer recruitment & registration
    // =====================================================================

    /**
     * Recruit (register) a new volunteer account with a preferred team type.
     * Password is required (min 6 chars) - the recruit signs in with it later.
     */
    @PostMapping("/volunteers/recruit")
    public ResponseEntity<?> recruitVolunteer(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireProgramManager(authHeader);

        String username = body.get("username");
        String password = body.get("password");
        String teamTypeStr = body.get("volunteerTeamType");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation Error", "message", "username and password are required."));
        }

        VolunteerTeamType teamType = null;
        if (teamTypeStr != null && !teamTypeStr.isBlank()) {
            try {
                teamType = VolunteerTeamType.valueOf(teamTypeTypeSafe(teamTypeStr));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Validation Error", "message", "Unknown volunteer team type: " + teamTypeStr));
            }
        }

        try {
            User created = userAuthService.registerPublic(username, password, Role.VOLUNTEER, teamType);
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Volunteer recruited successfully.",
                    "username", created.getUsername(),
                    "role", created.getRole().name(),
                    "volunteerTeamType", created.getVolunteerTeamType() != null
                            ? created.getVolunteerTeamType().name() : "UNASSIGNED",
                    "initialTrust", created.getTrustScore()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recruitment Failed", "message", e.getMessage()));
        }
    }

    private String teamTypeTypeSafe(String s) {
        return s.toUpperCase().trim();
    }

    /** Change an existing volunteer's team type (re-assignment). */
    @PostMapping("/volunteers/{username}/team-type")
    public ResponseEntity<?> changeVolunteerTeamType(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireProgramManager(authHeader);

        User volunteer = userAuthService.getUser(username);
        if (volunteer == null || volunteer.getRole() != Role.VOLUNTEER) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Not Found", "message", "Volunteer not found: " + username));
        }

        try {
            VolunteerTeamType type = VolunteerTeamType.valueOf(
                    body.getOrDefault("volunteerTeamType", "").toUpperCase().trim());
            boolean updated = userAuthService.updateVolunteerTeamType(username, type);
            if (!updated) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Rejected", "message", "Could not update volunteer team type."));
            }
            return ResponseEntity.ok(Map.of(
                    "message", "Volunteer " + username + " moved to " + type.getDisplayName(),
                    "volunteerTeamType", type.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Validation Error", "message", "Unknown volunteer team type."));
        }
    }

    /** Activate / deactivate a volunteer account (program management). */
    @PostMapping("/volunteers/{username}/activation")
    public ResponseEntity<?> toggleVolunteerActivation(
            @PathVariable String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String actor = requireProgramManager(authHeader);

        User volunteer = userAuthService.getUser(username);
        if (volunteer == null || volunteer.getRole() != Role.VOLUNTEER) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Not Found", "message", "Volunteer not found: " + username));
        }

        boolean updated = userAuthService.toggleUserStatus(username);
        if (!updated) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Rejected", "message", "Cannot toggle this account."));
        }

        User after = userAuthService.getUser(username);
        if (after != null && !after.isActive()) {
            tokenAuthService.revokeAllForUser(username);
        }
        return ResponseEntity.ok(Map.of(
                "message", "Volunteer " + username + (after.isActive() ? " activated." : " deactivated."),
                "active", after.isActive(),
                "by", actor));
    }

    // =====================================================================
    // 2. Disaster-area volunteers (proximity + status)
    // =====================================================================

    /**
     * Volunteers relevant to a disaster area: units near the incident coordinates
     * of the given disaster, plus their availability & mission-lock state.
     */
    @GetMapping("/disaster-area/{incidentId}")
    public ResponseEntity<?> getDisasterAreaVolunteers(
            @PathVariable String incidentId,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireProgramManager(authHeader);

        Incident incident = disasterService.getIncidentById(incidentId);
        if (incident == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Not Found", "message", "Incident not found: " + incidentId));
        }

        double lat = incident.getLatitude() != 0 ? incident.getLatitude() : 23.78;
        double lon = incident.getLongitude() != 0 ? incident.getLongitude() : 90.40;
        List<Map<String, Object>> nearby = disasterService.findNearbyTeams(lat, lon, radiusKm, null);

        // Keep only volunteer units in the listing
        List<Map<String, Object>> volunteers = nearby.stream()
                .filter(m -> String.valueOf(m.get("teamId")).startsWith("TEAM-VOL")
                        || String.valueOf(m.get("teamId")).startsWith("TEAM-ADHOC"))
                .toList();

        return ResponseEntity.ok(Map.of(
                "incidentId", incidentId,
                "location", incident.getLocation(),
                "latitude", lat,
                "longitude", lon,
                "radiusKm", radiusKm,
                "volunteerUnits", volunteers));
    }

    // =====================================================================
    // 3. Volunteer dispatching system (scoped: volunteer units only)
    // =====================================================================

    /** Dispatch volunteer units (optionally of one team type) to an incident. */
    @PostMapping("/dispatch/{incidentId}")
    public ResponseEntity<?> dispatchVolunteers(
            @PathVariable String incidentId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader)
            throws InvalidIncidentException, NoTeamAvailableException {
        String actor = requireProgramManager(authHeader);

        VolunteerTeamType filter = null;
        if (body != null && body.get("volunteerTeamType") != null && !body.get("volunteerTeamType").isBlank()) {
            filter = VolunteerTeamType.valueOf(body.get("volunteerTeamType").toUpperCase().trim());
        }

        ResponseTeam team = disasterService.assignTeam(incidentId, true, filter);
        Incident incident = disasterService.getIncidentById(incidentId);

        // Signal the dispatched unit so the volunteer portal lights up immediately
        disasterService.sendVolunteerSignal(
                VolunteerSignal.SignalType.DEPLOYMENT_CONFIRMED,
                team.getId(), null, incidentId,
                "Dispatched by " + actor + " to " + incident.getLocation() + ". Proceed to staging.",
                incident.getLocation(), actor);

        return ResponseEntity.ok(Map.of(
                "message", "Volunteer unit " + team.getId() + " dispatched to " + incidentId,
                "team", team,
                "incident", incident,
                "dispatchedBy", actor));
    }
}
