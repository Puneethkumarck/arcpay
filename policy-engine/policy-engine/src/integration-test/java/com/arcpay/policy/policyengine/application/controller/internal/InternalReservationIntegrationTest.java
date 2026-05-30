package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.policy.PolicyHashUtil;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.test.RestControllerAbstractTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalReservationIntegrationTest extends RestControllerAbstractTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private PolicyRepository policyRepository;

    @MockitoBean
    private AgentServiceClient agentServiceClient;

    @Test
    void shouldRejectReserveWithoutServiceAuth() throws Exception {
        // given
        var request = reserveRequest(UUID.randomUUID(), new BigDecimal("10.00"));

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/policies/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReserveHoldAndReturnApproved() throws Exception {
        // given
        var agentId = persistDailyLimitPolicy("1000.00");
        var request = reserveRequest(agentId, new BigDecimal("30.00"));

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/policies/reservations")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agentId.toString()))
                .andExpect(jsonPath("$.verdict").value("APPROVED"));
    }

    @Test
    void shouldCommitHeldReservationAndReturnCommittedStatus() throws Exception {
        // given
        var agentId = persistDailyLimitPolicy("1000.00");
        var paymentId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/internal/policies/reservations")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest(agentId, paymentId, new BigDecimal("30.00")))))
                .andExpect(status().isOk());

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/policies/reservations/{paymentId}/commit", paymentId)
                        .header("X-Service-Auth", SERVICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMMITTED"));
    }

    @Test
    void shouldReturnNotFoundWhenCommittingUnknownReservation() throws Exception {
        // given
        var paymentId = UUID.randomUUID();

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/policies/reservations/{paymentId}/commit", paymentId)
                        .header("X-Service-Auth", SERVICE_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCPAY-POLICY-0009"));
    }

    private UUID persistDailyLimitPolicy(String limit) {
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        List<PolicyRule> rules = List.of(new PolicyRule.DailyLimit(new BigDecimal(limit)));
        var hash = PolicyHashUtil.computePolicyHash(rules);
        policyRepository.save(Policy.builder()
                .policyId(UUID.randomUUID())
                .agentId(agentId)
                .ownerId(ownerId)
                .version(1)
                .rules(rules)
                .policyHash(hash)
                .status(PolicyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        given(agentServiceClient.getAgent(agentId))
                .willReturn(Optional.of(new AgentInfo(agentId, ownerId, "ACTIVE", hash)));
        return agentId;
    }

    private ReserveRequest reserveRequest(UUID agentId, BigDecimal amount) {
        return reserveRequest(agentId, UUID.randomUUID(), amount);
    }

    private ReserveRequest reserveRequest(UUID agentId, UUID paymentId, BigDecimal amount) {
        return ReserveRequest.builder()
                .paymentId(paymentId)
                .agentId(agentId)
                .recipientAddress(SOME_RECIPIENT)
                .amount(amount)
                .requestedAt(Instant.now())
                .build();
    }
}
