package com.disaster.controller;

import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.models.*;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Operational command endpoints for administrators: volunteer registry & signals,
 * structured incident verification, mission reports, and team management.
 */
@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final DisasterManagementService disasterService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public OpsController(DisasterManagementService disasterService, TokenAuthService tokenAuthService) {
        this.disasterService = disasterService;
        this.tokenAuthService = tokenAuthService;
    }

    private String requireAdmin(String authHeader) {
        tokenAuthService.requireRole(authHeader, Role.ADMIN);
        User admin = tokenAuthService.requireUser(authHeader);
        return admin.getUsername();
    }

    /** Program-manager scope (RESPONSE_TEAM or ADMIN) for volunteer-facing operations. */
    private String requireProgramManager(String authHeader) {
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.RESPONSE_TEAM);
        return tokenAuthService.requireUser(authHeader).getUsername();
    }

    // --- 1. Volunteer registry (grouped by team type) ---

    @GetMapping("/volunteers")
    public ResponseEntity<Map<String, Object>> getVolunteerRegistry(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.getVolunteerRegistry());
    }

    // --- 2. Signals (deployment confirmation etc.) ---

    @PostMapping("/volunteers/signal")
    public ResponseEntity<VolunteerSignal> sendSignal(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        // RESPONSE_TEAM coordinates volunteers daily; ADMIN retains full authority
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.RESPONSE_TEAM);
        String admin = tokenAuthService.requireUser(authHeader).getUsername();

        VolunteerSignal.SignalType type;
        try {
            type = VolunteerSignal.SignalType.valueOf(
                    body.getOrDefault("signalType", "DEPLOYMENT_CONFIRMED").toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidIncidentException("Invalid signal type.");
        }

        return ResponseEntity.ok(disasterService.sendVolunteerSignal(
                type,
                body.get("targetTeamId"),
                body.get("targetUsername"),
                body.get("incidentId"),
                body.get("message"),
                body.get("location"),
                admin));
    }

    @GetMapping("/volunteers/signals")
    public ResponseEntity<List<VolunteerSignal>> getAllSignals(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.getAllSignals());
    }

    // --- 3. Structured verification methods ---

    @PostMapping("/incidents/{id}/verify")
    public ResponseEntity<Map<String, Object>> runVerificationCheck(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        String admin = requireAdmin(authHeader);

        VerificationMethod method;
        try {
            method = VerificationMethod.valueOf(
                    String.valueOf(body.getOrDefault("method", "GPS_PINGS")).toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidIncidentException("Unknown verification method.");
        }

        boolean passed = Boolean.parseBoolean(String.valueOf(body.getOrDefault("passed", "true")));
        String notes = (String) body.get("notes");

        return ResponseEntity.ok(disasterService.runVerificationCheck(id, method, passed, notes, admin));
    }

    @GetMapping("/incidents/{id}/verify")
    public ResponseEntity<List<VerificationCheck>> getVerificationChecks(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.getVerificationChecks(id));
    }

    // --- 4. Mission reports ---

    @GetMapping("/mission-reports")
    public ResponseEntity<List<MissionReport>> getMissionReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.VOLUNTEER);
        return ResponseEntity.ok(disasterService.getAllMissionReports());
    }

    /** Requirement-matched AVAILABLE resources for a disaster type (shared with volunteers). */
    @GetMapping("/resources/matched")
    public ResponseEntity<List<Map<String, Object>>> getMatchedResources(
            @RequestParam String disasterType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.VOLUNTEER);
        DisasterType type;
        try {
            type = DisasterType.valueOf(disasterType.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidIncidentException("Unknown disaster type: " + disasterType);
        }
        return ResponseEntity.ok(disasterService.getMatchedAvailableResources(type));
    }

    // --- 5. Team operations & management ---

    @GetMapping("/teams/nearby")
    public ResponseEntity<List<Map<String, Object>>> findNearbyTeams(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(required = false) String skill,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.findNearbyTeams(lat, lon, radiusKm, skill));
    }

    @PostMapping("/teams/{id}/assemble")
    public ResponseEntity<ResponseTeam> assembleAdHocUnit(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        String admin = requireAdmin(authHeader);
        String unitName = (String) body.getOrDefault("unitName", "Ad-Hoc Unit");
        int memberCount = Integer.parseInt(String.valueOf(body.getOrDefault("memberCount", "4")));
        String leaderName = (String) body.get("leaderName");
        return ResponseEntity.ok(disasterService.assembleAdHocUnit(id, unitName, memberCount, leaderName, admin));
    }

    @PostMapping("/teams/{id}/leader")
    public ResponseEntity<ResponseTeam> assignLeader(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.assignTeamLeader(id, body.get("leaderName"), "admin"));
    }

    @PostMapping("/teams/{id}/skills")
    public ResponseEntity<ResponseTeam> setSkills(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.setTeamSkills(id, body.get("skillTags"), "admin"));
    }

    @PostMapping("/teams/{id}/position")
    public ResponseEntity<ResponseTeam> updatePosition(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        requireAdmin(authHeader);
        double lat = Double.parseDouble(String.valueOf(body.get("lat")));
        double lon = Double.parseDouble(String.valueOf(body.get("lon")));
        return ResponseEntity.ok(disasterService.updateUnitPosition(id, lat, lon, "admin"));
    }

    @PostMapping("/teams/{id}/mission-lock")
    public ResponseEntity<ResponseTeam> toggleMissionLock(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        requireAdmin(authHeader);
        return ResponseEntity.ok(disasterService.toggleMissionLock(id, "admin"));
    }

    @PostMapping("/teams/{id}/demobilize")
    public ResponseEntity<ResponseTeam> demobilize(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        // RESPONSE_TEAM manages the volunteer program, so they may stand units down too
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.RESPONSE_TEAM);
        User actor = tokenAuthService.requireUser(authHeader);
        return ResponseEntity.ok(disasterService.demobilizeUnit(id, actor.getUsername()));
    }

    @PostMapping("/volunteers/broadcast")
    public ResponseEntity<List<VolunteerSignal>> broadcast(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        String admin = requireProgramManager(authHeader);

        List<VolunteerTeamType> types = null;
        Object rawTypes = body.get("teamTypes");
        if (rawTypes instanceof List<?> list && !list.isEmpty()) {
            types = list.stream()
                    .map(t -> VolunteerTeamType.valueOf(String.valueOf(t).toUpperCase().trim()))
                    .toList();
        }

        String message = (String) body.get("message");
        if (message == null || message.isBlank()) {
            throw new InvalidIncidentException("Broadcast message is required.");
        }

        return ResponseEntity.ok(disasterService.broadcastToVolunteers(
                types, message, (String) body.get("incidentId"), admin));
    }
}
