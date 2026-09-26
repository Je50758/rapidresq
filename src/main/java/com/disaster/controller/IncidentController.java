package com.disaster.controller;

import com.disaster.dto.IncidentReportDto;
import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.NoTeamAvailableException;
import com.disaster.exceptions.ResourceNotFoundException;
import com.disaster.models.*;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for emergency incident reporting, priority triage retrieval,
 * citizen confirmation, and operational dispatch.
 * <p>
 * Mutating endpoints require a valid session token; dispatch / resolve / hoax
 * marking are additionally restricted to ADMIN and RESPONSE_TEAM roles.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final DisasterManagementService disasterService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public IncidentController(DisasterManagementService disasterService, TokenAuthService tokenAuthService) {
        this.disasterService = disasterService;
        this.tokenAuthService = tokenAuthService;
    }

    @GetMapping
    public ResponseEntity<List<Incident>> getAllIncidents() {
        return ResponseEntity.ok(disasterService.getAllIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncidentById(@PathVariable String id) {
        Incident incident = disasterService.getIncidentById(id);
        if (incident == null) {
            throw new ResourceNotFoundException("Incident not found with ID: " + id);
        }
        return ResponseEntity.ok(incident);
    }

    @PostMapping
    public ResponseEntity<Incident> reportIncident(
            @Valid @RequestBody IncidentReportDto dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {

        // Reporting requires a session; reporter identity always comes from the token.
        User currentUser = tokenAuthService.requireUser(authHeader);
        String reporter = currentUser.getUsername();
        int trustScore = currentUser.getTrustScore();

        String incidentId = disasterService.generateNextIncidentId();
        Incident incident;

        if (dto.getDisasterType() == null) {
            throw new InvalidIncidentException("Disaster type is required.");
        }

        switch (dto.getDisasterType()) {
            case FIRE:
                incident = new FireIncident(
                        incidentId,
                        dto.getLocation(),
                        dto.getInjuredCount(),
                        dto.getSeverity(),
                        reporter,
                        trustScore,
                        dto.getDescription(),
                        dto.getFireAlarmLevel(),
                        dto.getChemicalOrGasHazard()
                );
                break;
            case FLOOD:
                incident = new FloodIncident(
                        incidentId,
                        dto.getLocation(),
                        dto.getInjuredCount(),
                        dto.getSeverity(),
                        reporter,
                        trustScore,
                        dto.getDescription(),
                        dto.getWaterLevelMeters(),
                        dto.getStrandedPeopleCount()
                );
                break;
            case ACCIDENT:
                incident = new AccidentIncident(
                        incidentId,
                        dto.getLocation(),
                        dto.getInjuredCount(),
                        dto.getSeverity(),
                        reporter,
                        trustScore,
                        dto.getDescription(),
                        dto.getVehiclesInvolved(),
                        dto.getHighwayOrTrain()
                );
                break;
            case EARTHQUAKE:
                incident = new EarthquakeIncident(
                        incidentId,
                        dto.getLocation(),
                        dto.getInjuredCount(),
                        dto.getSeverity(),
                        reporter,
                        trustScore,
                        dto.getDescription(),
                        dto.getMagnitudeRichter(),
                        dto.getCollapsedBuildingsCount()
                );
                break;
            default:
                throw new InvalidIncidentException("Unsupported disaster type: " + dto.getDisasterType());
        }

        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            incident.setLatitude(dto.getLatitude());
            incident.setLongitude(dto.getLongitude());
        }

        Incident saved = disasterService.reportIncident(incident);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /**
     * Crowd confirmation. Requires a session; each account may confirm an incident once.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Incident> confirmIncident(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {

        User currentUser = tokenAuthService.requireUser(authHeader);
        Incident updated = disasterService.addConfirmingReport(id, currentUser.getUsername());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/assign-team")
    public ResponseEntity<Map<String, Object>> assignTeam(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader)
            throws InvalidIncidentException, NoTeamAvailableException {

        // RESPONSE_TEAM coordinates field operations and may dispatch the best-fit
        // professional unit; moderation stays admin-only (see /false-alarm).
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.RESPONSE_TEAM);

        ResponseTeam team = disasterService.assignTeam(id);
        Incident incident = disasterService.getIncidentById(id);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Team " + team.getName() + " successfully dispatched.");
        res.put("team", team);
        res.put("incident", incident);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveIncident(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {

        // Field crews close out their own missions; only moderators may penalize
        // reporters (false-alarm remains admin-only).
        tokenAuthService.requireRole(authHeader, Role.ADMIN, Role.RESPONSE_TEAM);

        User admin = tokenAuthService.requireUser(authHeader);
        Incident resolved = disasterService.resolveIncident(id, admin.getUsername());
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Incident marked as RESOLVED. Response units released.");
        res.put("incident", resolved);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/false-alarm")
    public ResponseEntity<Map<String, Object>> markFalseAlarm(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {

        tokenAuthService.requireRole(authHeader, Role.ADMIN);

        Incident incident = disasterService.markIncidentAsFalseAlarm(id);
        Map<String, Object> res = new HashMap<>();
        res.put("message", "Incident marked as FALSE ALARM. Reporter penalized -20 Trust Score.");
        res.put("incident", incident);
        return ResponseEntity.ok(res);
    }
}
