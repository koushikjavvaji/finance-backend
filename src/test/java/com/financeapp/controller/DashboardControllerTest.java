package com.financeapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.dto.request.AuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String analystToken;
    private String viewerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken   = loginAndGetToken("admin@finance.com",   "admin123");
        analystToken = loginAndGetToken("analyst@finance.com", "analyst123");
        viewerToken  = loginAndGetToken("viewer@finance.com",  "viewer123");
    }

    @Test
    void getSummary_asAdmin_returns200WithAllFields() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalIncome").exists())
                .andExpect(jsonPath("$.data.totalExpenses").exists())
                .andExpect(jsonPath("$.data.netBalance").exists())
                .andExpect(jsonPath("$.data.categoryTotals").exists())
                .andExpect(jsonPath("$.data.recentActivity").isArray())
                .andExpect(jsonPath("$.data.monthlyTrends").isArray());
    }

    @Test
    void getSummary_asAnalyst_returns200() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSummary_asViewer_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummary_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        AuthRequest.Login login = new AuthRequest.Login();
        login.setEmail(email);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }
}
