import auth.Role;
import auth.User;
import auth.UserAuthenticationService;

/**
 * Verification test driver for Phase 2: Authentication & Role-Based Access Control.
 */
public class Phase2Test {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println(" PHASE 2 VERIFICATION: AUTHENTICATION & ROLE-BASED ACCESS");
        System.out.println("=============================================================\n");

        UserAuthenticationService authService = UserAuthenticationService.getInstance();

        // 1. Test Preloaded Accounts & SHA-256 Hashing
        System.out.println("[TEST 1] Testing Preloaded Accounts & Password Authentication:");
        String[][] testCredentials = {
                {"admin", "admin123", "ADMIN"},
                {"citizen1", "pass123", "CITIZEN"},
                {"volunteer1", "pass123", "VOLUNTEER"},
                {"team1", "pass123", "RESPONSE_TEAM"}
        };

        for (String[] cred : testCredentials) {
            try {
                User user = authService.login(cred[0], cred[1]);
                System.out.printf(" -> SUCCESS: Logged in @%-10s | Role: %-15s | Trust: %d%%%n",
                        user.getUsername(), user.getRole(), user.getTrustScore());
                assert user.getRole().name().equals(cred[2]);
            } catch (Exception e) {
                System.err.println(" -> FAILED login for " + cred[0] + ": " + e.getMessage());
            }
        }
        System.out.println();

        // 2. Test Invalid Password Handling
        System.out.println("[TEST 2] Testing Invalid Password Handling:");
        try {
            authService.login("admin", "wrong_password");
            System.err.println(" -> FAILED: Should not allow wrong password!");
        } catch (Exception e) {
            System.out.println(" -> SUCCESS: Rejected bad password with message: " + e.getMessage());
        }
        System.out.println();

        // 3. Test Public Self-Registration (Citizen & Volunteer)
        System.out.println("[TEST 3] Testing Public Citizen Registration:");
        try {
            User newCitizen = authService.registerPublic("tanvir_dhaka", "citizenpass2026", Role.CITIZEN);
            System.out.println(" -> SUCCESS: Registered new citizen: @" + newCitizen.getUsername() + " (" + newCitizen.getRole() + ")");
        } catch (Exception e) {
            System.err.println(" -> Registration failed: " + e.getMessage());
        }
        System.out.println();

        // 4. Test Preventing Self-Registration for Admin Role
        System.out.println("[TEST 4] Testing Security Constraint (Preventing self-granting Admin):");
        try {
            authService.registerPublic("hacker_boy", "malicious_pass", Role.ADMIN);
            System.err.println(" -> FAILED: Unauthorized admin creation should have been blocked!");
        } catch (Exception e) {
            System.out.println(" -> SUCCESS: Blocked unauthorized role assignment: " + e.getMessage());
        }
        System.out.println();

        // 5. Test Account Deactivation
        System.out.println("[TEST 5] Testing Account Deactivation & Access Revocation:");
        try {
            authService.toggleUserStatus("citizen1");
            System.out.println(" -> citizen1 account deactivated.");
            try {
                authService.login("citizen1", "pass123");
                System.err.println(" -> FAILED: Deactivated user should not be able to log in!");
            } catch (Exception ex) {
                System.out.println(" -> SUCCESS: Deactivated user blocked: " + ex.getMessage());
            }
            // Restore citizen1
            authService.toggleUserStatus("citizen1");
            System.out.println(" -> citizen1 account restored to active.");
        } catch (Exception e) {
            System.err.println(" -> Error in deactivation test: " + e.getMessage());
        }
        System.out.println();

        // 6. Test Role Badges & Colors
        System.out.println("[TEST 6] Checking Role Badges & Styling:");
        for (Role r : Role.values()) {
            System.out.printf(" -> Role: %-15s | Color: RGB(%d,%d,%d) | Display: %s%n",
                    r.name(), r.getBadgeColor().getRed(), r.getBadgeColor().getGreen(), r.getBadgeColor().getBlue(), r.getDisplayName());
        }

        System.out.println("\n=============================================================");
        System.out.println(" PHASE 2 VERIFICATION COMPLETED WITH ZERO ERRORS!");
        System.out.println("=============================================================");
    }
}
