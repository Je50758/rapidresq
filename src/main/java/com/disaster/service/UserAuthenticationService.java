package com.disaster.service;

import com.disaster.exceptions.InvalidCredentialsException;
import com.disaster.models.Role;
import com.disaster.models.User;
import com.disaster.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user authentication, registration validations, and credential operations.
 * <p>
 * Authentication failures throw {@link InvalidCredentialsException} with a deliberately
 * generic message so attackers cannot enumerate valid usernames.
 */
@Service
public class UserAuthenticationService {
    private final UserRepository userRepository;

    @Autowired
    public UserAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        String key = username.trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findById(key);

        // Same generic failure message whether the user exists or the password is wrong.
        if (optionalUser.isEmpty() || !optionalUser.get().verifyPassword(password)) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        User user = optionalUser.get();
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated. Please contact an administrator.");
        }

        return user;
    }

    public synchronized User register(String username, String password, Role requestedRole, User requestingAdmin,
                                      com.disaster.models.VolunteerTeamType volunteerTeamType) {
        if (username == null || username.trim().length() < 3) {
            throw new InvalidCredentialsException("Username must be at least 3 characters long.");
        }
        if (password == null || password.length() < 6) {
            throw new InvalidCredentialsException("Password must be at least 6 characters long.");
        }

        String key = username.trim().toLowerCase();
        if (userRepository.existsById(key)) {
            throw new InvalidCredentialsException("Username '" + username + "' is already registered.");
        }

        Role assignedRole = requestedRole != null ? requestedRole : Role.CITIZEN;

        if (assignedRole == Role.ADMIN || assignedRole == Role.RESPONSE_TEAM) {
            if (requestingAdmin == null || requestingAdmin.getRole() != Role.ADMIN) {
                throw new InvalidCredentialsException("Only administrators can assign privileged roles (" + assignedRole + ").");
            }
        }

        int initialTrust = assignedRole == Role.VOLUNTEER ? 60 : 50;
        User newUser = new User(key, password, assignedRole, initialTrust);
        if (assignedRole == Role.VOLUNTEER && volunteerTeamType != null) {
            newUser.setVolunteerTeamType(volunteerTeamType);
        }
        return userRepository.save(newUser);
    }

    public synchronized User register(String username, String password, Role requestedRole, User requestingAdmin) {
        return register(username, password, requestedRole, requestingAdmin, null);
    }

    public synchronized User registerPublic(String username, String password, Role requestedRole,
                                            com.disaster.models.VolunteerTeamType volunteerTeamType) {
        if (requestedRole == Role.ADMIN || requestedRole == Role.RESPONSE_TEAM) {
            throw new InvalidCredentialsException("Self-registration is only permitted for Citizen or Volunteer roles.");
        }
        return register(username, password, requestedRole, null, volunteerTeamType);
    }

    public synchronized User registerPublic(String username, String password, Role requestedRole) {
        return registerPublic(username, password, requestedRole, null);
    }

    public List<User> getAllUsers() {
        List<User> list = userRepository.findAll();
        list.sort(Comparator.comparing(User::getUsername));
        return list;
    }

    public User getUser(String username) {
        if (username == null) return null;
        return userRepository.findById(username.trim().toLowerCase()).orElse(null);
    }

    public synchronized boolean updateUserRole(String username, Role newRole) {
        User user = getUser(username);
        if (user != null && newRole != null) {
            user.setRole(newRole);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public synchronized boolean toggleUserStatus(String username) {
        User user = getUser(username);
        if (user != null) {
            if (user.getUsername().equalsIgnoreCase("admin")) {
                return false;
            }
            boolean nowActive = !user.isActive();
            user.setActive(nowActive);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * Atomically adjusts a user's trust score by the given delta (clamped to 0-100).
     * The read-modify-write happens under the same lock as every other trust mutation,
     * preventing concurrent reward/penalty updates from being lost.
     */
    public synchronized boolean adjustTrustScore(String username, int delta) {
        User user = getUser(username);
        if (user != null) {
            user.adjustTrustScore(delta);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /** Program managers (RESPONSE_TEAM): re-assign a volunteer to a different team type. */
    public synchronized boolean updateVolunteerTeamType(String username, com.disaster.models.VolunteerTeamType teamType) {
        User user = getUser(username);
        if (user != null && user.getRole() == Role.VOLUNTEER && teamType != null) {
            user.setVolunteerTeamType(teamType);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public synchronized boolean updateTrustScore(String username, int newScore) {
        User user = getUser(username);
        if (user != null) {
            user.setTrustScore(newScore);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}
