package auth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton authentication and user management service.
 * Enforces SHA-256 validation, credential authentication, and role authorization rules.
 */
public class UserAuthenticationService {
    private static volatile UserAuthenticationService instance;

    private final Map<String, User> userRegistry;

    private UserAuthenticationService() {
        this.userRegistry = new ConcurrentHashMap<>();
        preloadDefaultAccounts();
    }

    public static UserAuthenticationService getInstance() {
        if (instance == null) {
            synchronized (UserAuthenticationService.class) {
                if (instance == null) {
                    instance = new UserAuthenticationService();
                }
            }
        }
        return instance;
    }

    /**
     * Authenticates credentials.
     * @throws Exception if username not found, password incorrect, or account deactivated.
     */
    public User login(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new Exception("Please enter your username.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new Exception("Please enter your password.");
        }

        String key = username.trim().toLowerCase();
        User user = userRegistry.get(key);

        if (user == null) {
            throw new Exception("User '" + username + "' does not exist.");
        }

        if (!user.isActive()) {
            throw new Exception("Account is deactivated. Please contact an administrator.");
        }

        if (!user.verifyPassword(password)) {
            throw new Exception("Invalid password. Please try again.");
        }

        return user;
    }

    /**
     * Registers a new user. Privileged roles (ADMIN, RESPONSE_TEAM) can only be granted
     * by an existing active administrator.
     */
    public synchronized User register(String username, String password, Role requestedRole, User requestingAdmin) throws Exception {
        if (username == null || username.trim().length() < 3) {
            throw new Exception("Username must be at least 3 characters long.");
        }
        if (password == null || password.length() < 6) {
            throw new Exception("Password must be at least 6 characters long.");
        }

        String key = username.trim().toLowerCase();
        if (userRegistry.containsKey(key)) {
            throw new Exception("Username '" + username + "' is already registered.");
        }

        Role assignedRole = requestedRole != null ? requestedRole : Role.CITIZEN;

        // Security check: Only Admin can grant ADMIN or RESPONSE_TEAM roles
        if ((assignedRole == Role.ADMIN || assignedRole == Role.RESPONSE_TEAM)) {
            if (requestingAdmin == null || requestingAdmin.getRole() != Role.ADMIN) {
                throw new Exception("Only administrators can assign privileged roles (" + assignedRole + "). Defaulting to Citizen.");
            }
        }

        int initialTrust = assignedRole == Role.VOLUNTEER ? 60 : 50;
        User newUser = new User(key, password, assignedRole, initialTrust);
        userRegistry.put(key, newUser);
        return newUser;
    }

    /**
     * Public self-registration (allows CITIZEN and VOLUNTEER roles).
     */
    public synchronized User registerPublic(String username, String password, Role requestedRole) throws Exception {
        if (requestedRole == Role.ADMIN || requestedRole == Role.RESPONSE_TEAM) {
            throw new Exception("Self-registration is only permitted for Citizen or Volunteer roles.");
        }
        return register(username, password, requestedRole, null);
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>(userRegistry.values());
        list.sort(Comparator.comparing(User::getUsername));
        return list;
    }

    public User getUser(String username) {
        if (username == null) return null;
        return userRegistry.get(username.trim().toLowerCase());
    }

    public synchronized boolean updateUserRole(String username, Role newRole) {
        User user = getUser(username);
        if (user != null && newRole != null) {
            user.setRole(newRole);
            return true;
        }
        return false;
    }

    public synchronized boolean toggleUserStatus(String username) {
        User user = getUser(username);
        if (user != null) {
            // Cannot deactivate the primary root admin
            if (user.getUsername().equalsIgnoreCase("admin")) {
                return false;
            }
            user.setActive(!user.isActive());
            return true;
        }
        return false;
    }

    public synchronized boolean updateTrustScore(String username, int newScore) {
        User user = getUser(username);
        if (user != null) {
            user.setTrustScore(newScore);
            return true;
        }
        return false;
    }

    private void preloadDefaultAccounts() {
        // Preload demo accounts explicitly requested
        userRegistry.put("admin", new User("admin", "admin123", Role.ADMIN, 100));
        userRegistry.put("citizen1", new User("citizen1", "pass123", Role.CITIZEN, 75));
        userRegistry.put("volunteer1", new User("volunteer1", "pass123", Role.VOLUNTEER, 85));
        userRegistry.put("team1", new User("team1", "pass123", Role.RESPONSE_TEAM, 90));
    }
}
