package com.disaster.controller;

import com.disaster.dto.AuthResponse;
import com.disaster.dto.LoginRequest;
import com.disaster.dto.RegisterRequest;
import com.disaster.models.User;
import com.disaster.service.TokenAuthService;
import com.disaster.service.UserAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication REST controller providing login, registration, and session identity endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAuthenticationService authService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public AuthController(UserAuthenticationService authService, TokenAuthService tokenAuthService) {
        this.authService = authService;
        this.tokenAuthService = tokenAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword());
        String token = tokenAuthService.createToken(user);
        AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getTrustScore(),
                "Authentication successful."
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User newUser = authService.registerPublic(request.getUsername(), request.getPassword(),
                request.getRole(), request.getVolunteerTeamType());
        String token = tokenAuthService.createToken(newUser);
        AuthResponse response = new AuthResponse(
                token,
                newUser.getUsername(),
                newUser.getRole(),
                newUser.getTrustScore(),
                "Registration successful."
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = tokenAuthService.getUserByToken(authHeader);
        if (user == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Invalid or expired session token.");
            return new ResponseEntity<>(err, HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        tokenAuthService.revokeToken(authHeader);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Logged out successfully.");
        return ResponseEntity.ok(res);
    }
}
