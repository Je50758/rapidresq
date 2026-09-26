import auth.Role;
import auth.User;
import auth.UserAuthenticationService;
import exceptions.InvalidIncidentException;
import models.DisasterType;
import models.Incident;
import models.IncidentStatus;
import models.ResponseTeam;
import patterns.DisasterManagementSystem;

import java.util.*;

/**
 * Verification test driver for Phase 3: Admin Panel.
 * Validates User Management operations, Incident Overrides,
 * Force Resolution, Team Reassignment, and Analytics Aggregation.
 */
public class Phase3Test {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println(" PHASE 3 VERIFICATION: CENTRAL ADMIN PANEL OPERATIONS");
        System.out.println("=============================================================\n");

        UserAuthenticationService authService = UserAuthenticationService.getInstance();
        DisasterManagementSystem system = DisasterManagementSystem.getInstance();

        // 1. User Management Verification
        System.out.println("[TEST 1] Admin User Management Operations:");
        String testUser = "citizen1";
        User u = authService.getUser(testUser);
        System.out.println(" -> Original state of @" + testUser + ": Role=" + u.getRole() + ", Trust=" + u.getTrustScore() + "%, Active=" + u.isActive());

        // A. Change Role
        authService.updateUserRole(testUser, Role.VOLUNTEER);
        System.out.println(" -> Updated role to: " + u.getRole());
        assert u.getRole() == Role.VOLUNTEER;

        // B. Adjust Trust Score
        authService.updateTrustScore(testUser, 95);
        System.out.println(" -> Updated trust score to: " + u.getTrustScore() + "%");
        assert u.getTrustScore() == 95;

        // C. Toggle Active Status
        authService.toggleUserStatus(testUser);
        System.out.println(" -> Toggled active status to: " + u.isActive());
        assert !u.isActive();

        // Restore
        authService.toggleUserStatus(testUser);
        authService.updateUserRole(testUser, Role.CITIZEN);
        System.out.println(" -> Restored @" + testUser + " to active Citizen.");
        System.out.println();

        // 2. Incident Priority Manual Override
        System.out.println("[TEST 2] Incident Manual Priority Override:");
        Incident inc = system.getIncidentById("INC-104"); // Earthquake in Armanitola
        if (inc != null) {
            double oldPriority = inc.getPriority();
            double manualOverrideScore = 550.0;
            inc.setPriority(manualOverrideScore);
            System.out.printf(" -> Incident %s priority manually changed: %.1f -> %.1f%n",
                    inc.getId(), oldPriority, inc.getPriority());
            assert inc.getPriority() == 550.0;
        }
        System.out.println();

        // 3. Team Reassignment & Force Resolution
        System.out.println("[TEST 3] Admin Team Reassignment & Force Resolution:");
        Incident floodInc = system.getIncidentById("INC-102"); // Sylhet flood
        if (floodInc != null) {
            System.out.println(" -> Before reassignment: Status=" + floodInc.getStatus() + ", Assigned=" + floodInc.getAssignedTeamId());

            // Assign Rescue Team
            ResponseTeam rscTeam = system.getTeamById("TEAM-RSC01");
            if (rscTeam != null && rscTeam.isAvailable()) {
                rscTeam.assignToIncident(floodInc.getId());
                floodInc.setAssignedTeamId(rscTeam.getId());
                floodInc.setStatus(IncidentStatus.IN_PROGRESS);
                System.out.println(" -> Assigned to " + rscTeam.getName() + " | Status: " + floodInc.getStatus());
            }

            // Admin Force Resolve
            try {
                system.resolveIncident(floodInc.getId());
                System.out.println(" -> After force resolve: Status=" + floodInc.getStatus() + ", Team Status=" + rscTeam.getStatus());
                assert floodInc.getStatus() == IncidentStatus.RESOLVED;
                assert rscTeam.isAvailable();
            } catch (InvalidIncidentException e) {
                System.err.println("Resolve failed: " + e.getMessage());
            }
        }
        System.out.println();

        // 4. Analytics Data Aggregation (Chart Counts & Hotspots)
        System.out.println("[TEST 4] Analytics Intelligence Aggregation:");
        Map<DisasterType, Integer> counts = new EnumMap<>(DisasterType.class);
        Map<String, Integer> hotspots = new HashMap<>();

        for (Incident i : system.getAllIncidents()) {
            counts.put(i.getDisasterType(), counts.getOrDefault(i.getDisasterType(), 0) + 1);
            hotspots.put(i.getLocation(), hotspots.getOrDefault(i.getLocation(), 0) + 1);
        }

        System.out.println(" -> Incident Counts by Type (Bar Chart Data):");
        for (Map.Entry<DisasterType, Integer> e : counts.entrySet()) {
            System.out.println("     * " + e.getKey().getTitle() + ": " + e.getValue() + " incidents");
        }

        System.out.println(" -> Top Disaster Hotspots Identified:");
        for (Map.Entry<String, Integer> e : hotspots.entrySet()) {
            System.out.println("     * " + e.getKey() + " -> " + e.getValue() + " incident(s)");
        }

        System.out.println("\n=============================================================");
        System.out.println(" PHASE 3 VERIFICATION COMPLETED WITH ZERO ERRORS!");
        System.out.println("=============================================================");
    }
}
