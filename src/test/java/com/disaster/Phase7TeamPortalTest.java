package com.disaster;

import com.disaster.dto.IncidentReportDto;
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
 * Phase 7: RESPONSE_TEAM as volunteer program manager + public community reports.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class Phase7TeamPortalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_TOKEN = "Bearer token-admin-123";
    private static final String TEAM_TOKEN = "Bearer token-team-123";
    private static final String CITIZEN_TOKEN = "Bearer token-citizen-123";

    @Test
    @DisplayName("TeamPortal: RESPONSE_TEAM can recruit a volunteer with a team type")
    void testRecruitVolunteer() throws Exception {
        mockMvc.perform(post("/api/team-portal/volunteers/recruit")
                        .header("Authorization", TEAM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"recruited_vol_a\",\"password\":\"volpass123\",\"volunteerTeamType\":\"LOGISTICS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("recruited_vol_a"))
                .andExpect(jsonPath("$.volunteerTeamType").value("LOGISTICS"));

        // The recruit can actually log in
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"recruited_vol_a\",\"password\":\"volpass123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TeamPortal: Citizen cannot recruit volunteers (403)")
    void testRecruitForbiddenForCitizen() throws Exception {
        mockMvc.perform(post("/api/team-portal/volunteers/recruit")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacker_vol\",\"password\":\"volpass123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TeamPortal: Re-assign a volunteer to another team type")
    void testChangeTeamType() throws Exception {
        mockMvc.perform(post("/api/team-portal/volunteers/volunteer2/team-type")
                        .header("Authorization", TEAM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerTeamType\":\"COMMUNICATION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volunteerTeamType").value("COMMUNICATION"));

        // Restore
        mockMvc.perform(post("/api/team-portal/volunteers/volunteer2/team-type")
                        .header("Authorization", TEAM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerTeamType\":\"MEDICAL_AID\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TeamPortal: Disaster-area volunteers listing near an incident")
    void testDisasterAreaVolunteers() throws Exception {
        mockMvc.perform(get("/api/team-portal/disaster-area/INC-102")
                        .param("radiusKm", "60")
                        .header("Authorization", TEAM_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value("INC-102"))
                .andExpect(jsonPath("$.volunteerUnits", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("TeamPortal: RESPONSE_TEAM dispatches a volunteer unit (with auto-signal)")
    void testVolunteerDispatchByResponseTeam() throws Exception {
        // Report a fresh incident
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Team Portal Dispatch Zone");
        dto.setInjuredCount(0);
        dto.setSeverity(Severity.LOW);
        dto.setWaterLevelMeters(1.0);
        dto.setStrandedPeopleCount(3);
        dto.setLatitude(23.79);
        dto.setLongitude(90.38);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // Dispatch a RESCUE volunteer unit as RESPONSE_TEAM
        MvcResult dispatched = mockMvc.perform(post("/api/team-portal/dispatch/" + id)
                        .header("Authorization", TEAM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerTeamType\":\"RESCUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team.id").value("TEAM-VOL01"))
                .andExpect(jsonPath("$.dispatchedBy").value("team1"))
                .andReturn();

        // Deployment signal was automatically created for the unit
        String body = dispatched.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("signal") || body.contains("message"));
    }

    @Test
    @DisplayName("TeamPortal: Volunteer dispatch respects team-type filter (409-ish empty result -> 503)")
    void testVolunteerDispatchTypeFilterMiss() throws Exception {
        IncidentReportDto dto = new IncidentReportDto();
        dto.setDisasterType(DisasterType.FIRE);
        dto.setLocation("Type Filter Miss Zone");
        dto.setSeverity(Severity.LOW);

        MvcResult created = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", CITIZEN_TOKEN)
                        .content(objectMapper.writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        // COMMUNICATION volunteer units cannot handle fire incidents -> no eligible unit
        mockMvc.perform(post("/api/team-portal/dispatch/" + id)
                        .header("Authorization", TEAM_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerTeamType\":\"COMMUNICATION\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Public: Mission reports are visible without authentication")
    void testPublicMissionReports() throws Exception {
        // Ensure at least one resolved incident exists
        mockMvc.perform(post("/api/incidents/INC-103/resolve")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/mission-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].availableResourcesJson", containsString("nearbySafeLocations")));
    }
}
