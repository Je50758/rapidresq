package com.disaster.controller;

import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.ResourceNotFoundException;
import com.disaster.models.*;
import com.disaster.observers.EmergencyEscalationNotifier;
import com.disaster.observers.StatusLogger;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import com.disaster.service.UserAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * REST controller providing administrative oversight, user role/trust control,
 * incident overrides, national 999 escalation logs, and aggregated disaster analytics.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DisasterManagementService disasterService;
    private final UserAuthenticationService authService;
    private final TokenAuthService tokenAuthService;
    private final EmergencyEscalationNotifier escalationNotifier;
    private final StatusLogger statusLogger;

    @Autowired
    public AdminController(DisasterManagementService disasterService,
                           UserAuthenticationService authService,
                           TokenAuthService tokenAuthService,
                           EmergencyEscalationNotifier escalationNotifier,
                           StatusLogger statusLogger) {
        this.disasterService = disasterService;
        this.authService = authService;
        this.tokenAuthService = tokenAuthService;
        this.escalationNotifier = escalationNotifier;
        this.statusLogger = statusLogger;
    }

    private void enforceAdmin(String authHeader) {
        // Strict check: throws 401 for missing/invalid tokens, 403 for non-admin roles,
        // and re-validates that the account is still ACTIVE.
        tokenAuthService.requireRole(authHeader, Role.ADMIN);
    }

    // --- User Administration ---

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/users/{username}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable String username,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);

        String roleStr = body.get("role");
        if (roleStr == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'role' is required.");
        }

        Role newRole;
        try {
            newRole = Role.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleStr);
        }

        boolean updated = authService.updateUserRole(username, newRole);
        if (!updated) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "User " + username + " role updated to " + newRole);
        res.put("user", authService.getUser(username));
        return ResponseEntity.ok(res);
    }

    @PutMapping("/users/{username}/trust")
    public ResponseEntity<Map<String, Object>> updateUserTrustScore(
            @PathVariable String username,
            @RequestBody Map<String, Integer> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);

        Integer newScore = body.get("trustScore");
        if (newScore == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'trustScore' is required.");
        }

        boolean updated = authService.updateTrustScore(username, Math.max(0, Math.min(100, newScore)));
        if (!updated) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "User " + username + " trust score updated to " + newScore);
        res.put("user", authService.getUser(username));
        return ResponseEntity.ok(res);
    }

    @PutMapping("/users/{username}/status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable String username,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);

        boolean updated = authService.toggleUserStatus(username);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot toggle status for: " + username);
        }

        User user = authService.getUser(username);
        if (user != null) {
            if (!user.isActive()) {
                // Deactivation instantly kills every live session of that account
                tokenAuthService.revokeAllForUser(user.getUsername());
            } else {
                // Re-arming keeps the demo tokens working after a toggle back to ACTIVE
                tokenAuthService.restoreDemoTokensIfPresent(java.util.List.of(user.getUsername()));
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "User " + username + " status changed to " + (user.isActive() ? "ACTIVE" : "DEACTIVATED"));
        res.put("user", user);
        return ResponseEntity.ok(res);
    }

    // --- Incident Overrides ---

    @PutMapping("/incidents/{id}/priority")
    public ResponseEntity<Incident> overrideIncidentPriority(
            @PathVariable String id,
            @RequestBody Map<String, Double> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        enforceAdmin(authHeader);

        Double priority = body.get("priority");
        if (priority == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'priority' is required.");
        }

        Incident updated = disasterService.overridePriority(id, priority);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/incidents/{id}/reassign")
    public ResponseEntity<Map<String, Object>> reassignIncidentTeam(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        enforceAdmin(authHeader);

        String teamId = body.get("teamId");
        if (teamId == null || teamId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field 'teamId' is required.");
        }

        ResponseTeam team = disasterService.reassignTeam(id, teamId);
        Incident incident = disasterService.getIncidentById(id);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Team " + team.getName() + " successfully assigned to incident " + id);
        res.put("team", team);
        res.put("incident", incident);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/incidents/{id}/force-resolve")
    public ResponseEntity<Map<String, Object>> forceResolveIncident(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        enforceAdmin(authHeader);

        Incident incident = disasterService.resolveIncident(id);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Incident " + id + " forcefully resolved by Administrator.");
        res.put("incident", incident);
        return ResponseEntity.ok(res);
    }

    // --- Specialized Features: Escalation & Audit Logs ---

    @GetMapping("/escalation-logs")
    public ResponseEntity<List<EscalationLog>> getEscalationLogs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);
        return ResponseEntity.ok(escalationNotifier.getEscalationLogs());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<StatusLogger.LogEntry>> getAuditLogs(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);
        return ResponseEntity.ok(statusLogger.getLogs());
    }

    /**
     * ADMIN-ONLY donor registry with fully decrypted donor contact details
     * (name, phone, address). Raw donation listing is also restricted to admins.
     */
    @GetMapping("/donors")
    public ResponseEntity<List<Map<String, Object>>> getDonorRegistry(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        enforceAdmin(authHeader);
        return ResponseEntity.ok(disasterService.getDonorRegistry());
    }

    // --- Aggregated Analytics ---

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Aggregates include hotspot locations and donation totals: any ACTIVE signed-in
        // role may view them, but anonymous callers are rejected (401).
        tokenAuthService.requireUser(authHeader);

        List<Incident> allIncidents = disasterService.getAllIncidents();
        List<ResponseTeam> allTeams = disasterService.getAllResponseTeams();
        List<ReliefNeed> allRelief = disasterService.getAllReliefNeeds();

        long activeCount = allIncidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED)
                .count();

        long resolvedCount = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED)
                .count();

        long verifiedCount = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.VERIFIED || i.getStatus() == IncidentStatus.IN_PROGRESS)
                .count();

        long availableTeams = allTeams.stream()
                .filter(ResponseTeam::isAvailable)
                .count();

        long dispatchedTeams = allTeams.stream()
                .filter(t -> !t.isAvailable())
                .count();

        double totalDonationsRaised = allRelief.stream()
                .mapToDouble(ReliefNeed::getCollectedFundingBDT)
                .sum();

        double totalGoal = allRelief.stream()
                .mapToDouble(ReliefNeed::getTargetFundingBDT)
                .sum();

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalIncidents", allIncidents.size());
        analytics.put("activeIncidents", activeCount);
        analytics.put("resolvedIncidents", resolvedCount);
        analytics.put("verifiedIncidents", verifiedCount);
        analytics.put("incidentCountsByType", disasterService.getIncidentCountsByType());
        analytics.put("hotspots", disasterService.getHotspots());
        analytics.put("averageResponseTimeMinutes", disasterService.getAverageResponseTimeMinutes());
        analytics.put("totalTeams", allTeams.size());
        analytics.put("availableTeams", availableTeams);
        analytics.put("dispatchedTeams", dispatchedTeams);
        analytics.put("totalDonationsRaised", totalDonationsRaised);
        analytics.put("totalDonationGoal", totalGoal);
        analytics.put("totalReliefCampaigns", allRelief.size());
        analytics.put("totalEscalations", escalationNotifier.getEscalationLogs().size());

        return ResponseEntity.ok(analytics);
    }
}
