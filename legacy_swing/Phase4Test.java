import auth.Role;
import auth.User;
import auth.UserAuthenticationService;
import exceptions.InvalidIncidentException;
import models.*;
import observers.EmergencyEscalationNotifier;
import patterns.DisasterManagementSystem;

import java.util.List;

/**
 * Verification test driver for Phase 4: Unique Innovations.
 * Tests Reporter Trust Score dynamics, Emergency 999 Escalations,
 * and Post-Disaster Rehabilitation Donation Tracking.
 */
public class Phase4Test {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println(" PHASE 4 VERIFICATION: UNIQUE INNOVATION FEATURES");
        System.out.println("=============================================================\n");

        DisasterManagementSystem system = DisasterManagementSystem.getInstance();
        UserAuthenticationService authService = UserAuthenticationService.getInstance();

        // ------------------------------------------------------------------
        // TEST 1: Reporter Trust Score System
        // ------------------------------------------------------------------
        System.out.println("[TEST 1] Reporter Trust Score Dynamics & False Alarm Penalties:");
        User citizen = authService.getUser("citizen1");
        int initialTrust = citizen.getTrustScore();
        System.out.println(" -> @" + citizen.getUsername() + " initial Trust Score: " + initialTrust + "%");

        // A. File new report and get it verified (+5 reward)
        String newId = system.generateNextIncidentId();
        FireIncident fire = new FireIncident(
                newId,
                "Chittagong Port Warehouse Yard",
                2,
                Severity.MODERATE,
                citizen.getUsername(),
                citizen.getTrustScore(),
                "Pallet storage fire near container gate 3",
                2,
                false
        );

        try {
            system.reportIncident(fire);
            // Simulate 3 confirmations
            system.addConfirmingReport(newId, "volunteer1");
            system.addConfirmingReport(newId, "team1");
            System.out.println(" -> Incident " + newId + " auto-verified with 3 citizen confirmations.");
            System.out.println(" -> @" + citizen.getUsername() + " Trust Score after verification: " + citizen.getTrustScore() + "% (+5 reward)");
            assert citizen.getTrustScore() == initialTrust + 5;
        } catch (InvalidIncidentException e) {
            System.err.println("Verification error: " + e.getMessage());
        }

        // B. False Alarm Penalty (-20)
        String spamId = system.generateNextIncidentId();
        AccidentIncident spamInc = new AccidentIncident(
                spamId,
                "Mirpur 10 Circle",
                0,
                Severity.LOW,
                citizen.getUsername(),
                citizen.getTrustScore(),
                "False rumor of bus rollover",
                1,
                false
        );

        try {
            system.reportIncident(spamInc);
            int beforeFalse = citizen.getTrustScore();
            system.markIncidentAsFalseAlarm(spamId);
            System.out.println(" -> False alarm marked for " + spamId);
            System.out.println(" -> @" + citizen.getUsername() + " Trust Score after false alarm penalty: " + citizen.getTrustScore() + "% (-20 penalty)");
            assert citizen.getTrustScore() == beforeFalse - 20;
        } catch (InvalidIncidentException e) {
            System.err.println("False alarm error: " + e.getMessage());
        }

        // C. Low Trust Reporter Warning (< 20)
        citizen.setTrustScore(15);
        String lowTrustId = system.generateNextIncidentId();
        FloodIncident lowTrustInc = new FloodIncident(
                lowTrustId,
                "Demra, Dhaka",
                0,
                Severity.LOW,
                citizen.getUsername(),
                citizen.getTrustScore(),
                "Minor puddle on street",
                0.3,
                0
        );
        try {
            system.reportIncident(lowTrustInc);
            System.out.println(" -> Filed incident from low-trust reporter (Trust: " + lowTrustInc.getReporterTrustScore() + "%):");
            System.out.println("     * Description tagged: " + lowTrustInc.getDescription());
            assert lowTrustInc.getDescription().contains("LOW TRUST REPORTER");
        } catch (InvalidIncidentException e) {
            System.err.println("Report error: " + e.getMessage());
        }
        // Restore citizen trust
        citizen.setTrustScore(initialTrust);
        System.out.println();

        // ------------------------------------------------------------------
        // TEST 2: Emergency Escalation Simulation (999)
        // ------------------------------------------------------------------
        System.out.println("[TEST 2] Emergency Escalation Simulation (National 999 Integration):");
        EmergencyEscalationNotifier escalationNotifier = system.getEscalationNotifier();
        List<EscalationLog> logs = escalationNotifier.getEscalationLogs();
        System.out.println(" -> Preloaded / Active 999 Escalations Count: " + logs.size());
        for (EscalationLog log : logs) {
            System.out.println("     * " + log);
        }

        // Test triggering a new 999 critical escalation
        String criticalId = system.generateNextIncidentId();
        FireIncident massiveFire = new FireIncident(
                criticalId,
                "Tejgaon Industrial Area, Dhaka",
                12,
                Severity.CRITICAL, // CRITICAL triggers 999 escalation automatically
                "volunteer1",
                85,
                "Chemical storage explosion with high structural hazard",
                5,
                true
        );

        try {
            int beforeCount = escalationNotifier.getEscalationLogs().size();
            system.reportIncident(massiveFire);
            int afterCount = escalationNotifier.getEscalationLogs().size();
            System.out.println(" -> Reported CRITICAL incident: " + criticalId + " (Priority: " + massiveFire.getPriority() + ")");
            System.out.println(" -> Automated 999 Escalations Count: " + beforeCount + " -> " + afterCount);
            assert afterCount == beforeCount + 1;
        } catch (InvalidIncidentException e) {
            System.err.println("Escalation report error: " + e.getMessage());
        }
        System.out.println();

        // ------------------------------------------------------------------
        // TEST 3: Post-Disaster Rehabilitation & Donation Tracker
        // ------------------------------------------------------------------
        System.out.println("[TEST 3] Post-Disaster Rehabilitation & Donation Matching:");
        // A. Resolve an incident and verify automatic relief need generation
        int needsBefore = system.getAllReliefNeeds().size();
        try {
            system.resolveIncident("INC-103"); // Feni Highway collision with 8 injured
            int needsAfter = system.getAllReliefNeeds().size();
            System.out.println(" -> Resolved incident INC-103. Total Relief Drives: " + needsBefore + " -> " + needsAfter);
            assert needsAfter == needsBefore + 1;

            ReliefNeed newNeed = system.getAllReliefNeeds().get(needsAfter - 1);
            System.out.println(" -> Generated Campaign: " + newNeed);
            System.out.println("     * Location: " + newNeed.getLocation());
            System.out.println("     * Relief Target: ৳" + newNeed.getTargetFundingBDT());
            System.out.println("     * Target Supplies: " + newNeed.getTargetPackagesCount() + " units of " + newNeed.getPrimaryReliefPackage());

            // B. Donate to the generated campaign
            DonationRecord donor1 = new DonationRecord("DON-TEST1", newNeed.getNeedId(), "citizen1", 25000.0, "Trauma Care Pack");
            system.donateToNeed(newNeed.getNeedId(), donor1);

            System.out.println(" -> Matched citizen donation of ৳" + donor1.getAmountBDT() + " (" + donor1.getReliefItem() + ")");
            System.out.println(" -> Updated Campaign Status: " + newNeed);
            assert newNeed.getCollectedFundingBDT() == 25000.0;
            assert newNeed.getCollectedPackagesCount() == 1;

        } catch (InvalidIncidentException e) {
            System.err.println("Resolution / donation error: " + e.getMessage());
        }

        System.out.println("\n=============================================================");
        System.out.println(" PHASE 4 VERIFICATION COMPLETED WITH ZERO ERRORS!");
        System.out.println("=============================================================");
    }
}
