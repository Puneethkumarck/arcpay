package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.RecordSpendingRequest;
import com.arcpay.policy.policyengine.test.RestControllerAbstractTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalSpendingLedgerIntegrationTest extends RestControllerAbstractTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RecordSpendingRequest request(UUID agentId, UUID paymentId, BigDecimal amount, Instant executedAt) {
        return RecordSpendingRequest.builder()
                .agentId(agentId)
                .paymentId(paymentId)
                .amount(amount)
                .recipient(SOME_RECIPIENT)
                .executedAt(executedAt)
                .build();
    }

    @Test
    void shouldRecordAndReturnSpendingEndToEnd() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var executedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);

        // when

        // then
        mockMvc.perform(post("/api/v1/internal/spending-ledger")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request(agentId, paymentId, new BigDecimal("50.00"), executedAt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));

        assertThat(countByPaymentId(paymentId)).isOne();
    }

    @Test
    void shouldBeIdempotentOnDuplicatePaymentId() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var executedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        record(agentId, paymentId, new BigDecimal("50.00"), executedAt);

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/spending-ledger")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                request(agentId, paymentId, new BigDecimal("999.00"), executedAt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50.000000));

        assertThat(countByPaymentId(paymentId)).isOne();
    }

    @Test
    void shouldReturnCorrectSpendingSummary() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        var within24Hours = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var within7Days = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        record(agentId, UUID.randomUUID(), new BigDecimal("10.00"), within24Hours);
        record(agentId, UUID.randomUUID(), new BigDecimal("20.00"), within7Days);

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

    private void record(UUID agentId, UUID paymentId, BigDecimal amount, Instant executedAt) throws Exception {
        mockMvc.perform(post("/api/v1/internal/spending-ledger")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(agentId, paymentId, amount, executedAt))))
                .andExpect(status().isOk());
    }

    private int countByPaymentId(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spending_ledger WHERE payment_id = ?", Integer.class, paymentId);
    }
}
