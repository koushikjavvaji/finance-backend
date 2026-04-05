package com.financeapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeapp.dto.request.AuthRequest;
import com.financeapp.dto.request.TransactionRequest;
import com.financeapp.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken  = loginAndGetToken("admin@finance.com",   "admin123");
        viewerToken = loginAndGetToken("viewer@finance.com",  "viewer123");
    }

    @Test
    void getAll_asViewer_returns200() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        TransactionRequest req = validTransactionRequest();

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("Test Category"))
                .andExpect(jsonPath("$.data.type").value("INCOME"));
    }

    @Test
    void create_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransactionRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withNegativeAmount_returns400() throws Exception {
        TransactionRequest req = validTransactionRequest();
        req.setAmount(new BigDecimal("-100.00"));

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.amount").exists());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransactionRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_withTypeFilter_returnsFilteredResults() throws Exception {
        mockMvc.perform(get("/api/transactions?type=INCOME")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void delete_asAdmin_softDeletesRecord() throws Exception {
        // First create a transaction
        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validTransactionRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("data").get("id").asLong();

        // Now delete it
        mockMvc.perform(delete("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify it's no longer accessible
        mockMvc.perform(get("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
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

    private TransactionRequest validTransactionRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setType(TransactionType.INCOME);
        req.setCategory("Test Category");
        req.setDate(LocalDate.now());
        req.setNotes("Test note");
        return req;
    }
}
