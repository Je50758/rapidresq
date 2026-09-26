package com.disaster.controller;

import com.disaster.dto.DonationDto;
import com.disaster.exceptions.InvalidIncidentException;
import com.disaster.exceptions.ResourceNotFoundException;
import com.disaster.models.DonationRecord;
import com.disaster.models.ReliefNeed;
import com.disaster.models.User;
import com.disaster.service.DisasterManagementService;
import com.disaster.service.TokenAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for post-disaster community rehabilitation campaigns and donation pledges.
 * <p>
 * Donating requires a session. Donor contact details (name, phone, address) are collected,
 * AES-256-GCM encrypted at rest, and only exposed in decrypted form through the
 * admin-only donor registry ({@code GET /api/admin/donors}). Public responses show
 * masked names (e.g. "Farhan K.").
 */
@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DisasterManagementService disasterService;
    private final TokenAuthService tokenAuthService;

    @Autowired
    public DonationController(DisasterManagementService disasterService,
                              TokenAuthService tokenAuthService) {
        this.disasterService = disasterService;
        this.tokenAuthService = tokenAuthService;
    }

    /** Donation history is sensitive; only administrators may browse raw records. */
    @GetMapping
    public ResponseEntity<List<DonationRecord>> getAllDonations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        tokenAuthService.requireRole(authHeader, com.disaster.models.Role.ADMIN);
        return ResponseEntity.ok(disasterService.getAllDonations());
    }

    @GetMapping("/needs")
    public ResponseEntity<List<ReliefNeed>> getAllReliefNeeds() {
        return ResponseEntity.ok(disasterService.getAllReliefNeeds());
    }

    @GetMapping("/needs/{id}")
    public ResponseEntity<ReliefNeed> getReliefNeedById(@PathVariable String id) {
        ReliefNeed need = disasterService.getReliefNeedById(id);
        if (need == null) {
            throw new ResourceNotFoundException("Relief campaign not found with ID: " + id);
        }
        return ResponseEntity.ok(need);
    }

    /**
     * Original contribute route (kept for API compatibility).
     */
    @PostMapping("/contribute")
    public ResponseEntity<Map<String, Object>> contribute(
            @Valid @RequestBody DonationDto dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        return doContribute(dto, authHeader);
    }

    /**
     * Nested alias matching the frontend client: POST /api/donations/needs/{id}/contribute.
     * The path variable overrides the body's reliefNeedId to keep both routes consistent.
     */
    @PostMapping("/needs/{needId}/contribute")
    public ResponseEntity<Map<String, Object>> contributeToNeed(
            @PathVariable String needId,
            @Valid @RequestBody DonationDto dto,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws InvalidIncidentException {
        dto.setReliefNeedId(needId);
        return doContribute(dto, authHeader);
    }

    private ResponseEntity<Map<String, Object>> doContribute(DonationDto dto, String authHeader)
            throws InvalidIncidentException {

        if (dto.getReliefNeedId() == null || dto.getReliefNeedId().trim().isEmpty()) {
            throw new ResourceNotFoundException("Relief campaign ID is required.");
        }

        User user = tokenAuthService.requireUser(authHeader);

        // Donor display name: explicit input wins; anonymous hides it entirely;
        // otherwise default to the account username.
        String donorName = dto.isAnonymous() ? "Anonymous"
                : (dto.getDonorName() != null && !dto.getDonorName().isBlank()
                    ? dto.getDonorName().trim()
                    : user.getUsername());

        DonationRecord record = new DonationRecord(
                null, // ID generated collision-free by the service
                dto.getReliefNeedId(),
                user.getUsername(),
                dto.getAmountBDT(),
                dto.getReliefItem()
        );
        record.setDonorName(donorName);

        DonationRecord saved = disasterService.donateToNeed(
                dto.getReliefNeedId(),
                record,
                dto.getDonorPhone(),
                dto.getDonorAddress(),
                dto.getPaymentChannel(),
                dto.isAnonymous()
        );
        ReliefNeed updatedNeed = disasterService.getReliefNeedById(dto.getReliefNeedId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Donation of ৳" + String.format("%.2f", saved.getAmountBDT()) + " successfully pledged. Thank you!");
        response.put("donation", saved); // masked public view - PII never serialized
        response.put("reliefNeed", updatedNeed);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
