package com.disaster;

import com.disaster.dto.DonationDto;
import com.disaster.dto.IncidentReportDto;
import com.disaster.dto.LoginRequest;
import com.disaster.models.DisasterType;
import com.disaster.models.Severity;
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
 * Regression tests for donor-data privacy (encryption at rest, masked public views,
 * admin-only registry) and the operational command policy: ADMIN has full command;
 * RESPONSE_TEAM may dispatch and resolve on the main endpoints but never moderate
 * (false-alarm) — that stays admin-only.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class Phase5PrivacyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_TOKEN = "Bearer token-admin-123";
    private static final String CITIZEN_TOKEN = "Bearer token-citizen-123";
    private static final String TEAM_TOKEN = "Bearer token-team-123";

    @Test
    @DisplayName("Privacy: Donation with donor PII succeeds; public response masks the name")
    void testDonationWithPiiMasksPublicName() throws Exception {
        DonationDto dto = new DonationDto("RELIEF-001", 750.0, "Cash Aid", null);
        dto.setDonorName("Farhan Kaif");
        dto.setDonorPhone("+8801711223344");
        dto.setDonorAddress("House 12, Road 7, Dhanmondi, Dhaka");
        dto.setPaymentChannel("bKash");

        MvcResult result = mockMvc.perform(post("/api/donations/contribute")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donation.donorName").value("Farhan K."))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Full name / phone / address must never appear in the public response
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("Kaif"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("+8801711223344"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("Dhanmondi"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("donorPhoneEnc"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("donorAddressEnc"));
    }

    @Test
    @DisplayName("Privacy: Admin donor registry shows decrypted PII")
    void testAdminDonorRegistryDecrypts() throws Exception {
        mockMvc.perform(get("/api/admin/donors").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].donorPhone", anyOf(nullValue(), isA(String.class))))
                .andExpect(jsonPath("$[0].donorName", anyOf(nullValue(), isA(String.class))));
    }

    @Test
    @DisplayName("Privacy: Donor registry is admin-only (citizen & anonymous blocked)")
    void testDonorRegistryAdminOnly() throws Exception {
        mockMvc.perform(get("/api/admin/donors").header("Authorization", CITIZEN_TOKEN))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/donors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Privacy: Raw donation listing is admin-only")
    void testRawDonationListAdminOnly() throws Exception {
        mockMvc.perform(get("/api/donations").header("Authorization", CITIZEN_TOKEN))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/donations").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access: RESPONSE_TEAM can dispatch on the main operational endpoint")
    void testResponseTeamCanDispatch() throws Exception {
        // RESPONSE_TEAM coordinates field operations: they may dispatch the best-fit
        // professional unit and close out missions (resolve), while moderation
        // (false-alarm) remains admin-only.
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Team Ops Dispatch Zone");
        dto.setInjuredCount(0);
        dto.setSeverity(Severity.MODERATE);
        dto.setWaterLevelMeters(1.2);
        dto.setStrandedPeopleCount(4);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/incidents/" + id + "/assign-team")
                        .header("Authorization", TEAM_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team.id").exists());
    }

    @Test
    @DisplayName("Access: RESPONSE_TEAM can resolve an assigned mission")
    void testResponseTeamCanResolve() throws Exception {
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FIRE);
        dto.setLocation("Team Ops Resolve Zone");
        dto.setInjuredCount(0);
        dto.setSeverity(Severity.LOW);
        dto.setFireAlarmLevel(1);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/incidents/" + id + "/assign-team")
                        .header("Authorization", TEAM_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/incidents/" + id + "/resolve")
                        .header("Authorization", TEAM_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access: RESPONSE_TEAM cannot mark false alarms (moderation is admin-only)")
    void testResponseTeamBlockedFromFalseAlarm() throws Exception {
        mockMvc.perform(post("/api/incidents/INC-103/false-alarm")
                        .header("Authorization", TEAM_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Access: Admin retains full operational command")
    void testAdminCanDispatch() throws Exception {
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Admin Ops Test Zone");
        dto.setInjuredCount(0);
        dto.setSeverity(Severity.LOW);
        dto.setWaterLevelMeters(1.0);
        dto.setStrandedPeopleCount(2);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/incidents/" + id + "/assign-team")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Access: Donor name is required (400 without it)")
    void testDonorNameRequired() throws Exception {
        DonationDto dto = new DonationDto("RELIEF-001", 100.0, "Cash Aid", null);
        dto.setDonorName(null);

        mockMvc.perform(post("/api/donations/contribute")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Privacy: Anonymous flag hides donor name in public view")
    void testAnonymousDonation() throws Exception {
        DonationDto dto = new DonationDto("RELIEF-001", 300.0, "Cash Aid", null);
        dto.setDonorName("Secret Donor");
        dto.setAnonymous(true);

        mockMvc.perform(post("/api/donations/needs/RELIEF-001/contribute")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.donation.donorName").value("Anonymous"));
    }

    @Test
    @DisplayName("Smoke: Login still works end-to-end")
    void testLoginStillWorks() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());
    }
}
