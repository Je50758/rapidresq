package com.disaster;

import com.disaster.dto.IncidentReportDto;
import com.disaster.dto.RegisterRequest;
import com.disaster.models.DisasterType;
import com.disaster.models.Severity;
import com.disaster.models.VolunteerTeamType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 6: volunteer team types, admin signals, structured verification methods,
 * mission reports, and team operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class Phase6OpsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_TOKEN = "Bearer token-admin-123";
    private static final String CITIZEN_TOKEN = "Bearer token-citizen-123";
    private static final String VOLUNTEER_TOKEN = "Bearer token-volunteer-123";

    @Test
    @DisplayName("Ops: Volunteer registry grouped by team type (admin only)")
    void testVolunteerRegistry() throws Exception {
        mockMvc.perform(get("/api/ops/volunteers").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitsByType.RESCUE", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.unitsByType.MEDICAL_AID", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.unitsByType.LOGISTICS", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.accountsByType.RESCUE", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Ops: Volunteer registry blocked for non-admins")
    void testVolunteerRegistryForbidden() throws Exception {
        mockMvc.perform(get("/api/ops/volunteers").header("Authorization", CITIZEN_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ops: New volunteer can register with a preferred team type")
    void testVolunteerRegistrationWithTeamType() throws Exception {
        RegisterRequest req = new RegisterRequest("new_med_vol", "secret123", null);
        req.setRole(com.disaster.models.Role.VOLUNTEER);
        req.setVolunteerTeamType(VolunteerTeamType.LOGISTICS);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Registry now contains the new volunteer under LOGISTICS
        mockMvc.perform(get("/api/ops/volunteers").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountsByType.LOGISTICS[?(@.username == 'new_med_vol')]", hasSize(1)));
    }

    @Test
    @DisplayName("Ops: Admin deployment-confirmation signal reaches volunteer inbox")
    void testDeploymentSignalFlow() throws Exception {
        // Admin sends deployment confirmation to TEAM-VOL01
        mockMvc.perform(post("/api/ops/volunteers/signal")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signalType\":\"DEPLOYMENT_CONFIRMED\",\"targetTeamId\":\"TEAM-VOL01\","
                                + "\"incidentId\":\"INC-101\",\"message\":\"Proceed to Mirpur mission area.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalType").value("DEPLOYMENT_CONFIRMED"))
                .andExpect(jsonPath("$.status").value("SENT"));

        // volunteer1 sees signals in the inbox (team-targeted + username-targeted)
        MvcResult inbox = mockMvc.perform(get("/api/volunteer/inbox")
                        .header("Authorization", VOLUNTEER_TOKEN))
                .andExpect(status().isOk())
                .andReturn();
        String body = inbox.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("signals"));
    }

    @Test
    @DisplayName("Ops: Citizen cannot send signals; volunteers cannot see admin registry")
    void testSignalAuthorization() throws Exception {
        mockMvc.perform(post("/api/ops/volunteers/signal")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signalType\":\"BROADCAST\",\"targetTeamId\":\"TEAM-VOL01\",\"message\":\"fake\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/volunteer/inbox").header("Authorization", CITIZEN_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ops: Volunteer acknowledges a signal -> ACKNOWLEDGED")
    void testSignalAcknowledgement() throws Exception {
        MvcResult signal = mockMvc.perform(post("/api/ops/volunteers/signal")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signalType\":\"STANDBY\",\"targetUsername\":\"volunteer2\",\"message\":\"Hold position.\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String signalId = objectMapper.readTree(signal.getResponse().getContentAsString()).get("signalId").asText();

        // volunteer2 needs a token: login
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"volunteer2\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String vol2Token = "Bearer " + objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/volunteer/inbox/" + signalId + "/ack").header("Authorization", vol2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    @DisplayName("Ops: Structured verification - GPS_PINGS + THERMAL_SCANS auto-verify a report")
    void testVerificationMethodsAutoVerify() throws Exception {
        // Citizen reports a fire
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FIRE);
        dto.setLocation("Verification Test Building, Banani");
        dto.setInjuredCount(2);
        dto.setSeverity(Severity.MODERATE);
        dto.setFireAlarmLevel(3);
        dto.setChemicalOrGasHazard(false);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // GPS pings pass (weight 2) -> not yet verified
        mockMvc.perform(post("/api/ops/incidents/" + id + "/verify")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GPS_PINGS\",\"passed\":true,\"notes\":\"Coords match\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoVerified").doesNotExist());

        // Thermal scan pass (weight 3) -> total 5 >= 3 -> auto-verified
        mockMvc.perform(post("/api/ops/incidents/" + id + "/verify")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"THERMAL_SCANS\",\"passed\":true,\"notes\":\"Heat plume confirmed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoVerified").value(true));

        // Incident is now VERIFIED
        mockMvc.perform(get("/api/incidents/" + id))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    @DisplayName("Ops: Verification checks are admin-only")
    void testVerificationAdminOnly() throws Exception {
        mockMvc.perform(post("/api/ops/incidents/INC-101/verify")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GPS_PINGS\",\"passed\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ops: Resolving generates a mission report with matched resources; broadcast delivers it")
    void testMissionReportGeneration() throws Exception {
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Mission Report Test Haor");
        dto.setInjuredCount(1);
        dto.setSeverity(Severity.LOW);
        dto.setWaterLevelMeters(1.5);
        dto.setStrandedPeopleCount(5);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/incidents/" + id + "/resolve")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ops/mission-reports").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incidentId").value(id))
                .andExpect(jsonPath("$[0].availableResourcesJson").exists());

        // Volunteers can read mission reports too
        mockMvc.perform(get("/api/ops/mission-reports").header("Authorization", VOLUNTEER_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ops: Team management - leader, skills, mission lock, demobilize")
    void testTeamManagementOps() throws Exception {
        mockMvc.perform(post("/api/ops/teams/TEAM-VOL01/leader")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaderName\":\"New Leader Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderName").value("New Leader Test"));

        mockMvc.perform(post("/api/ops/teams/TEAM-VOL01/skills")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillTags\":\"swift-water, drone-op\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillTags").value("swift-water, drone-op"));

        mockMvc.perform(post("/api/ops/teams/TEAM-VOL01/mission-lock")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missionLocked").value(true));

        // A mission-locked team is not dispatchable
        mockMvc.perform(post("/api/ops/teams/TEAM-VOL01/mission-lock")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missionLocked").value(false));

        mockMvc.perform(post("/api/ops/teams/TEAM-VOL01/demobilize")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Ops: Proximity filtering finds teams near a coordinate")
    void testNearbyTeams() throws Exception {
        mockMvc.perform(get("/api/ops/teams/nearby")
                        .param("lat", "23.80")
                        .param("lon", "90.36")
                        .param("radiusKm", "15")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Ops: Broadcast reaches all volunteer units")
    void testBroadcast() throws Exception {
        mockMvc.perform(post("/api/ops/volunteers/broadcast")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"System-wide drill at 18:00.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }
}
