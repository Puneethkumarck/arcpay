package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.test.RestControllerAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalSpendingLedgerIntegrationTest extends RestControllerAbstractTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnCorrectSpendingSummary() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        var within24Hours = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var within7Days = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        seedLedger(agentId, UUID.randomUUID(), new BigDecimal("10.00"), within24Hours);
        seedLedger(agentId, UUID.randomUUID(), new BigDecimal("20.00"), within7Days);

        // when
        // then
        mockMvc.perform(get("/api/v1/internal/agents/{agentId}/spending-summary", agentId)
                        .header("X-Service-Auth", SERVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.dailyTotal").value(10.000000))
                .andExpect(jsonPath("$.weeklyTotal").value(30.000000))
                .andExpect(jsonPath("$.transactionCount24h").value(1));
    }

    @Test
    void shouldRequireServiceAuthForSummary() throws Exception {
        // given
        var agentId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(get("/api/v1/internal/agents/{agentId}/spending-summary", agentId))
                .andExpect(status().isUnauthorized());
    }

    private void seedLedger(UUID agentId, UUID paymentId, BigDecimal amount, Instant executedAt) {
        jdbcTemplate.update("""
                INSERT INTO spending_ledger (entry_id, agent_id, payment_id, amount, recipient, executed_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, now())
                """, UUID.randomUUID(), agentId, paymentId, amount, SOME_RECIPIENT, Timestamp.from(executedAt));
    }
}
