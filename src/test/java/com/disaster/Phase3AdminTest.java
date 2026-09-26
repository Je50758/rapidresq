package com.disaster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class Phase3AdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_TOKEN = "Bearer token-admin-123";
    private static final String CITIZEN_TOKEN = "Bearer token-citizen-123";

    @Test
    @DisplayName("Admin: Access user list with Admin Token")
    void testGetUsersAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[?(@.username == 'admin')].role").value("ADMIN"));
    }

    @Test
    @DisplayName("Admin: Non-admin citizen is blocked with 403 Forbidden")
    void testGetUsersForbiddenForCitizen() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", CITIZEN_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin: Update user trust score")
    void testUpdateTrustScore() throws Exception {
        Map<String, Integer> payload = new HashMap<>();
        payload.put("trustScore", 92);

        mockMvc.perform(put("/api/admin/users/citizen1/trust")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.trustScore").value(92));
    }

    @Test
    @DisplayName("Admin: Toggle user account status")
    void testToggleUserStatus() throws Exception {
        mockMvc.perform(put("/api/admin/users/citizen1/status")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.active").value(false));

        // Re-enable for other tests
        mockMvc.perform(put("/api/admin/users/citizen1/status")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.active").value(true));
    }

    @Test
    @DisplayName("Admin: Override incident priority")
    void testOverridePriority() throws Exception {
        Map<String, Double> payload = new HashMap<>();
        payload.put("priority", 350.0);

        mockMvc.perform(put("/api/admin/incidents/INC-101/priority")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(350.0));
    }

    @Test
    @DisplayName("Admin: Reassign incident to specific team")
    void testReassignTeam() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("teamId", "TEAM-VOL01"); // Mirpur Community First Responders

        mockMvc.perform(post("/api/admin/incidents/INC-101/reassign")
                        .header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident.assignedTeamId").value("TEAM-VOL01"));
    }

    @Test
    @DisplayName("Admin: View 999 Escalation Audit Logs")
    void testGetEscalationLogs() throws Exception {
        mockMvc.perform(get("/api/admin/escalation-logs")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Analytics: Aggregated metrics calculation")
    void testGetAnalytics() throws Exception {
        mockMvc.perform(get("/api/admin/analytics")
                        .header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").isNumber())
                .andExpect(jsonPath("$.activeIncidents").isNumber())
                .andExpect(jsonPath("$.hotspots").isArray())
                .andExpect(jsonPath("$.incidentCountsByType.FIRE").isNumber())
                .andExpect(jsonPath("$.averageResponseTimeMinutes").isNumber());
    }
}
