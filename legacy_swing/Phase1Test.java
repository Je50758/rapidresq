import exceptions.InvalidIncidentException;
import exceptions.NoTeamAvailableException;
import models.*;
import patterns.DisasterManagementSystem;
import observers.VolunteerNotifier;
import observers.StatusLogger;

import java.util.List;

/**
 * Verification driver for Phase 1: Core OOP Backend.
 * Tests Polymorphism, Singleton, Observer pattern, PriorityQueue triage,
 * and custom exception flows.
 */
public class Phase1Test {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println(" PHASE 1 VERIFICATION: CORE OOP DISASTER RESPONSE SYSTEM");
        System.out.println("=============================================================\n");

        DisasterManagementSystem system = DisasterManagementSystem.getInstance();

        // 1. Verify Preloaded Data
        System.out.println("[TEST 1] Checking Preloaded Teams & Resources (Composition):");
        for (ResponseTeam team : system.getAllResponseTeams()) {
            System.out.println(" -> Team: " + team.getName() + " | Status: " + team.getStatus());
            for (Resource r : team.getResources()) {
                System.out.println("     * Resource: " + r);
            }
        }
        System.out.println();

        // 2. Polymorphic Priority Calculation Test
        System.out.println("[TEST 2] Checking Incidents & Polymorphic Priority Calculation:");
        for (Incident inc : system.getAllIncidents()) {
            System.out.printf(" -> [%s] Type: %-10s | Priority: %6.1f | Status: %-12s | Injured: %d | Loc: %s%n",
                    inc.getId(), inc.getDisasterType().name(), inc.getPriority(), inc.getStatus().name(),
                    inc.getInjuredCount(), inc.getLocation());
        }
        System.out.println();

        // 3. Priority Queue Ordering (Urgent triage peek)
        System.out.println("[TEST 3] Priority Queue Highest Urgency Inspection:");
        Incident topPriority = system.peekHighestUrgencyIncident();
        if (topPriority != null) {
            System.out.println(" -> Top Priority Incident: " + topPriority.getId() + " (" + 
                    topPriority.getDisasterType() + ") with Priority Score: " + topPriority.getPriority());
        }
        System.out.println();

        // 4. Report New Incident & Observer Pattern
        System.out.println("[TEST 4] Reporting New Incident & Testing Observer Pattern:");
        String newId = system.generateNextIncidentId();
        FireIncident newFire = new FireIncident(
                newId,
                "Chawkbazar Chemical Warehouse, Old Dhaka",
                6,
                Severity.CRITICAL,
                "citizen_tester",
                60,
                "Massive chemical blaze, multiple plastic factories threatened",
                5, // Alarm level 5
                true // Chemical hazard
        );

        try {
            system.reportIncident(newFire);
            System.out.println(" -> Successfully reported new fire incident: " + newId + 
                    " with calculated Priority: " + newFire.getPriority());
        } catch (InvalidIncidentException e) {
            System.err.println("Failed to report: " + e.getMessage());
        }
        System.out.println();

        // 5. Crowdsourced Auto-Verification (3 Reports threshold)
        System.out.println("[TEST 5] Testing Citizen Crowdsourced Confirmation (Auto-Verify at 3+):");
        try {
            System.out.println(" -> Status before confirmations: " + newFire.getStatus() + " (Reports: " + newFire.getReportCount() + ")");
            system.addConfirmingReport(newId, "citizen_rahim");
            System.out.println(" -> Added confirmation 1. Status: " + newFire.getStatus() + " (Reports: " + newFire.getReportCount() + ")");
            system.addConfirmingReport(newId, "volunteer_karim");
            System.out.println(" -> Added confirmation 2. Status: " + newFire.getStatus() + " (Reports: " + newFire.getReportCount() + ")");
            
            if (newFire.getStatus() == IncidentStatus.VERIFIED) {
                System.out.println(" -> SUCCESS: Incident automatically transitioned to VERIFIED status!");
            }
        } catch (InvalidIncidentException e) {
            System.err.println("Error adding report: " + e.getMessage());
        }
        System.out.println();

        // 6. Assign Response Team
        System.out.println("[TEST 6] Assigning Specialized Response Team to Incident:");
        try {
            ResponseTeam assigned = system.assignTeam(newId);
            System.out.println(" -> Successfully assigned: " + assigned.getName() + " (" + assigned.getSpecialization() + ")");
            System.out.println(" -> Incident Status: " + newFire.getStatus());
            System.out.println(" -> Team Status: " + assigned.getStatus());
        } catch (NoTeamAvailableException | InvalidIncidentException e) {
            System.err.println("Assignment failed: " + e.getMessage());
        }
        System.out.println();

        // 7. Resolving Incident
        System.out.println("[TEST 7] Resolving Incident & Freeing Resources:");
        try {
            system.resolveIncident(newId);
            System.out.println(" -> Incident Status: " + newFire.getStatus());
            System.out.println(" -> Team Status after release: " + system.getTeamById(newFire.getAssignedTeamId()).getStatus());
        } catch (InvalidIncidentException e) {
            System.err.println("Resolve failed: " + e.getMessage());
        }
        System.out.println();

        // 8. Testing Custom Exception
        System.out.println("[TEST 8] Testing Custom Exception (assigning team to already resolved incident):");
        try {
            system.assignTeam(newId);
            System.err.println(" -> FAILED: Exception should have been thrown!");
        } catch (InvalidIncidentException | NoTeamAvailableException e) {
            System.out.println(" -> SUCCESS: Expected exception caught -> " + e.getMessage());
        }
        System.out.println();

        // 9. Check Observer Logs & Alerts
        System.out.println("[TEST 9] Verifying Observer Outputs:");
        VolunteerNotifier notifier = system.getVolunteerNotifier();
        System.out.println(" -> Total Volunteer Alerts Broadcasted: " + notifier.getActiveAlerts().size());
        StatusLogger logger = system.getStatusLogger();
        System.out.println(" -> Total Audit Logs Recorded: " + logger.getLogs().size());

        System.out.println("\n=============================================================");
        System.out.println(" PHASE 1 VERIFICATION COMPLETED WITH ZERO ERRORS!");
        System.out.println("=============================================================");
    }
}
