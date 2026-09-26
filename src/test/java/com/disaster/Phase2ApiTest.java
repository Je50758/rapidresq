package com.disaster;

import com.disaster.dto.DonationDto;
import com.disaster.dto.IncidentReportDto;
import com.disaster.dto.LoginRequest;
import com.disaster.dto.RegisterRequest;
import com.disaster.models.DisasterType;
import com.disaster.models.Role;
import com.disaster.models.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase2ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify Authentication: Login with Demo Accounts")
    void testLoginDemoAccounts() throws Exception {
        LoginRequest loginReq = new LoginRequest("admin", "admin123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Verify Authentication: Public Self-Registration")
    void testPublicRegistration() throws Exception {
        RegisterRequest regReq = new RegisterRequest("new_citizen_api", "secretpass123", Role.CITIZEN);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_citizen_api"))
                .andExpect(jsonPath("$.role").value("CITIZEN"));
    }

    @Test
    @DisplayName("Verify Incidents: Report Polymorphic Incident & Retrieve Priority Queue")
    void testIncidentEndpoints() throws Exception {
        // 1. Report new flood incident
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Sunamganj Sadar Haor Area");
        dto.setInjuredCount(2);
        dto.setSeverity(Severity.MODERATE);
        dto.setDescription("Flash flood submerging rural homesteads");
        dto.setWaterLevelMeters(2.5);
        dto.setStrandedPeopleCount(45);

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer token-citizen-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.location").value("Sunamganj Sadar Haor Area"))
                .andExpect(jsonPath("$.priority").isNumber());

        // 2. Retrieve all incidents
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Verify Response Teams & Composed Equipment")
    void testTeamsEndpoint() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resources").isArray());
    }

    @Test
    @DisplayName("Verify Volunteers & Broadcast Emergency Alerts")
    void testVolunteersEndpoint() throws Exception {
        mockMvc.perform(get("/api/volunteers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volunteerUnits").isArray())
                .andExpect(jsonPath("$.activeBroadcastAlerts").isArray());
    }

    @Test
    @DisplayName("Verify Post-Disaster Rehabilitation & Donation Matching")
    void testDonationsEndpoint() throws Exception {
        DonationDto dto = new DonationDto("RELIEF-001", 5000.0, "Clean Water Kits", "citizen1");

        mockMvc.perform(post("/api/donations/contribute")
                        .header("Authorization", "Bearer token-citizen-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donation.amountBDT").value(5000.0));
    }
}
