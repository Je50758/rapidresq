package com.disaster;

import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.NoTeamAvailableException;
import com.disaster.models.*;
import com.disaster.repository.IncidentRepository;
import com.disaster.repository.ResponseTeamRepository;
import com.disaster.repository.UserRepository;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.UserAuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase1BackendTest {

    @Autowired
    private DisasterManagementService disasterService;

    @Autowired
    private UserAuthenticationService userAuthService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ResponseTeamRepository responseTeamRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify Spring Context & Preloaded Database Entities")
    void testPreloadedEntities() {
        // 1. Verify Users
        assertTrue(userRepository.count() >= 4, "At least 4 demo users should be preloaded");
        assertNotNull(userAuthService.getUser("admin"));
        assertEquals(Role.ADMIN, userAuthService.getUser("admin").getRole());

        // 2. Verify Teams with Resources (Composition)
        List<ResponseTeam> teams = responseTeamRepository.findAll();
        assertFalse(teams.isEmpty(), "Teams should be preloaded");
        ResponseTeam fb = teams.stream().filter(t -> t instanceof FireBrigade).findFirst().orElse(null);
        assertNotNull(fb, "FireBrigade team should exist");
        assertFalse(fb.getResources().isEmpty(), "Team should hold composed resources");

        // 3. Verify Incidents
        List<Incident> incidents = incidentRepository.findAll();
        assertTrue(incidents.size() >= 4, "Demo incidents should be present in database");
    }

    @Test
    @DisplayName("Verify Polymorphic Priority & Auto-Verification (3 Reports)")
    void testPolymorphismAndAutoVerification() throws Exception {
        String testId = "INC-TEST-01";
        FireIncident fire = new FireIncident(
                testId,
                "Mohakhali Wireless Gate, Dhaka",
                3,
                Severity.CRITICAL,
                "citizen1",
                75,
                "Commercial chemical warehouse fire",
                4,
                true
        );

        // Save through service
        Incident reported = disasterService.reportIncident(fire);
        assertEquals(testId, reported.getId());
        assertEquals(IncidentStatus.REPORTED, reported.getStatus());
        assertTrue(reported.getPriority() > 100.0, "Critical fire should have substantial priority");

        // Add 2 citizen confirmations -> reaches 3 total reports
        disasterService.addConfirmingReport(testId, "volunteer1");
        Incident verified = disasterService.addConfirmingReport(testId, "citizen1");

        assertEquals(IncidentStatus.VERIFIED, verified.getStatus(), "Incident should automatically transition to VERIFIED at 3 reports");
    }

    @Test
    @DisplayName("Verify Response Team Assignment & Incident Resolution")
    void testTeamAssignmentAndResolution() throws Exception {
        String testId = "INC-TEST-02";
        AccidentIncident accident = new AccidentIncident(
                testId,
                "Airport Road, Uttara, Dhaka",
                5,
                Severity.CRITICAL,
                "volunteer1",
                85,
                "Multi-vehicle pileup",
                3,
                true
        );

        disasterService.reportIncident(accident);

        // Assign team
        ResponseTeam assigned = disasterService.assignTeam(testId);
        assertNotNull(assigned);
        assertEquals(TeamStatus.ASSIGNED, assigned.getStatus());

        Incident active = disasterService.getIncidentById(testId);
        assertEquals(IncidentStatus.IN_PROGRESS, active.getStatus());
        assertEquals(assigned.getId(), active.getAssignedTeamId());

        // Resolve incident
        Incident resolved = disasterService.resolveIncident(testId);
        assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());

        ResponseTeam released = disasterService.getTeamById(assigned.getId());
        assertEquals(TeamStatus.AVAILABLE, released.getStatus(), "Team should be freed back to AVAILABLE");
    }

    @Test
    @DisplayName("Verify Observer Alerts & Custom Exception Handling")
    void testObserversAndExceptionHandling() {
        // Verify observers recorded logs
        assertFalse(disasterService.getStatusLogger().getLogs().isEmpty());
        assertFalse(disasterService.getVolunteerNotifier().getActiveAlerts().isEmpty());

        // Verify custom exception on invalid operation
        assertThrows(InvalidIncidentException.class, () -> {
            disasterService.resolveIncident("NON-EXISTENT-ID");
        });
    }
}
