package com.disaster.service;

import com.disaster.exceptions.InvalidCredentialsException;
import com.disaster.models.Role;
import com.disaster.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight token-based authentication service.
 * Maps session tokens to authenticated User entities without Spring Security boilerplate.
 * <p>
 * Hardening added:
 * <ul>
 *   <li>Tokens carry an absolute expiry (default 24h) and are rejected afterwards.</li>
 *   <li>Every token lookup re-checks that the backing account is still ACTIVE, so
 *       deactivating a user instantly invalidates their live sessions.</li>
 *   <li>{@link #revokeAllForUser} lets admin actions kill all sessions of a user.</li>
 *   <li>Expired entries are evicted on access to bound memory growth.</li>
 * </ul>
 */
@Service
public class TokenAuthService {

    private static final long TOKEN_TTL_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private static class Session {
        final String username;
        final long expiresAtMs;

        Session(String username, long expiresAtMs) {
            this.username = username;
            this.expiresAtMs = expiresAtMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    private final UserAuthenticationService userAuthService;
    private final Map<String, Session> tokenToSession = new ConcurrentHashMap<>();

    @Autowired
    public TokenAuthService(UserAuthenticationService userAuthService) {
        this.userAuthService = userAuthService;
        // Pre-configure convenient demo tokens (same 24h TTL applies; refreshed on startup)
        long expiry = System.currentTimeMillis() + TOKEN_TTL_MS;
        tokenToSession.put("token-admin-123", new Session("admin", expiry));
        tokenToSession.put("token-citizen-123", new Session("citizen1", expiry));
        tokenToSession.put("token-volunteer-123", new Session("volunteer1", expiry));
        tokenToSession.put("token-team-123", new Session("team1", expiry));
    }

    public String createToken(User user) {
        String token = "rrq-" + UUID.randomUUID().toString().replace("-", "");
        tokenToSession.put(token, new Session(user.getUsername(), System.currentTimeMillis() + TOKEN_TTL_MS));
        return token;
    }

    public User getUserByToken(String token) {
        Session session = resolveSession(token);
        if (session == null) return null;

        User user = userAuthService.getUser(session.username);
        // Re-check liveness on every use: a deactivated account loses all its sessions immediately.
        if (user == null || !user.isActive()) {
            if (user == null) {
                tokenToSession.values().removeIf(s -> s.username.equals(session.username));
            }
            return null;
        }
        return user;
    }

    /**
     * Resolves the session for a token, enforcing expiry and evicting dead entries.
     */
    private Session resolveSession(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        Session session = tokenToSession.get(token);
        if (session == null) return null;

        if (session.isExpired()) {
            // Evict only if still the same expired entry (avoid racing a fresh re-login)
            tokenToSession.remove(token, session);
            return null;
        }
        return session;
    }

    /**
     * Role-gated helper for protected endpoints. Returns false when the token is
     * missing/invalid, the account is deactivated, or the role does not match.
     */
    public boolean hasRole(String token, Role... allowedRoles) {
        User user = getUserByToken(token);
        if (user == null) return false;
        for (Role r : allowedRoles) {
            if (user.getRole() == r) return true;
        }
        return false;
    }

    /**
     * Strict variant used by endpoints that MUST have a valid session.
     * Throws 401-mapped {@link InvalidCredentialsException} instead of silently returning null.
     */
    public User requireUser(String token) {
        User user = getUserByToken(token);
        if (user == null) {
            throw new InvalidCredentialsException("Missing, invalid, or expired session token.");
        }
        return user;
    }

    /**
     * Strict role check for protected endpoints; throws 403-mapped AccessDeniedException.
     */
    public void requireRole(String token, Role... allowedRoles) {
        User user = requireUser(token);
        for (Role r : allowedRoles) {
            if (user.getRole() == r) return;
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Access Denied: one of the following roles is required: " + String.join(", ",
                        java.util.Arrays.stream(allowedRoles).map(Enum::name).toArray(String[]::new)));
    }

    public void revokeToken(String token) {
        if (token != null) {
            if (token.startsWith("Bearer ")) token = token.substring(7).trim();
            tokenToSession.remove(token);
        }
    }

    /**
     * Kills every active session belonging to the given username.
     * Called when an administrator deactivates an account.
     */
    public void revokeAllForUser(String username) {
        if (username == null) return;
        tokenToSession.values().removeIf(s -> s.username.equalsIgnoreCase(username));
        // The hardcoded demo token must not survive a deactivation either.
        if ("admin".equalsIgnoreCase(username)) tokenToSession.remove("token-admin-123");
        if ("citizen1".equalsIgnoreCase(username)) tokenToSession.remove("token-citizen-123");
        if ("volunteer1".equalsIgnoreCase(username)) tokenToSession.remove("token-volunteer-123");
        if ("team1".equalsIgnoreCase(username)) tokenToSession.remove("token-team-123");
    }

    /**
     * Re-arms the pre-configured demo tokens after a deactivation is undone
     * (keeps the demo experience working across status toggles).
     */
    public void restoreDemoTokensIfPresent(java.util.List<String> usernames) {
        if (usernames == null) return;
        long expiry = System.currentTimeMillis() + TOKEN_TTL_MS;
        if (usernames.contains("admin")) tokenToSession.putIfAbsent("token-admin-123", new Session("admin", expiry));
        if (usernames.contains("citizen1")) tokenToSession.putIfAbsent("token-citizen-123", new Session("citizen1", expiry));
        if (usernames.contains("volunteer1")) tokenToSession.putIfAbsent("token-volunteer-123", new Session("volunteer1", expiry));
        if (usernames.contains("team1")) tokenToSession.putIfAbsent("token-team-123", new Session("team1", expiry));
    }
}
