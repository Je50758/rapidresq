package com.disaster.controller;

import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.ResourceNotFoundException;
import com.disaster.models.ResponseTeam;
import com.disaster.models.Role;
import com.disaster.models.User;
import com.disaster.models.Volunteer;
import com.disaster.models.VolunteerSignal;
import com.disaster.models.VolunteerTeamType;
import com.disaster.observers.VolunteerNotifier;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for emergency response teams, the community volunteer network,
 * and the volunteer self-service portal (signal inbox, acknowledgements).
 */
@RestController
@RequestMapping("/api")
public class TeamController {

    private final DisasterManagementService disasterService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public TeamController(DisasterManagementService disasterService, TokenAuthService tokenAuthService) {
        this.disasterService = disasterService;
        this.tokenAuthService = tokenAuthService;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<ResponseTeam>> getAllTeams() {
        return ResponseEntity.ok(disasterService.getAllResponseTeams());
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<ResponseTeam> getTeamById(@PathVariable String id) {
        ResponseTeam team = disasterService.getTeamById(id);
        if (team == null) {
            throw new ResourceNotFoundException("Response team not found with ID: " + id);
        }
        return ResponseEntity.ok(team);
    }

    /** Team types a new volunteer can join (public - shown on the registration page). */
    @GetMapping("/volunteer-types")
    public ResponseEntity<List<Map<String, Object>>> getVolunteerTeamTypes() {
        List<Map<String, Object>> types = Arrays.stream(VolunteerTeamType.values())
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", t.name());
                    item.put("label", t.getIcon() + " " + t.getDisplayName());
                    item.put("description", t.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(types);
    }

    @GetMapping("/volunteers")
    public ResponseEntity<Map<String, Object>> getVolunteersAndAlerts() {
        List<Volunteer> volunteerCorps = disasterService.getAllResponseTeams().stream()
                .filter(t -> t instanceof Volunteer)
                .map(t -> (Volunteer) t)
                .collect(Collectors.toList());

        List<VolunteerNotifier.VolunteerAlert> alerts = disasterService.getVolunteerNotifier().getActiveAlerts();

        Map<String, Object> response = new HashMap<>();
        response.put("volunteerUnits", volunteerCorps);
        response.put("activeBroadcastAlerts", alerts);
        return ResponseEntity.ok(response);
    }

    /**
     * VOLUNTEER PORTAL: the signed-in volunteer's signal inbox - deployment confirmations,
     * standby orders, broadcasts and mission reports addressed to them or their team.
     */
    @GetMapping("/volunteer/inbox")
    public ResponseEntity<?> getMyInbox(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = tokenAuthService.requireUser(authHeader);
        if (user.getRole() != Role.VOLUNTEER) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Only volunteers have a signal inbox."));
        }

        // Signals addressed to the username + signals sent to volunteer units of their team type
        List<VolunteerSignal> signals = disasterService.getInboxForUser(user);

        Map<String, Object> inbox = new HashMap<>();
        inbox.put("username", user.getUsername());
        inbox.put("teamType", user.getVolunteerTeamType());
        inbox.put("signals", signals);
        inbox.put("unreadCount", signals.stream().filter(s -> s.getStatus() == VolunteerSignal.SignalStatus.SENT).count());
        return ResponseEntity.ok(inbox);
    }

    /** Volunteer acknowledges a signal (deployment confirmation read & accepted). */
    @PostMapping("/volunteer/inbox/{signalId}/ack")
    public ResponseEntity<?> acknowledgeSignal(
            @PathVariable String signalId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        User user = tokenAuthService.requireUser(authHeader);
        if (user.getRole() != Role.VOLUNTEER) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Only volunteers can acknowledge signals."));
        }
        return ResponseEntity.ok(disasterService.acknowledgeSignal(signalId, user.getUsername()));
    }
}
