package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.api.model.InternalEvaluateRequest;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.policy.PolicyHashUtil;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
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

class InternalPolicyEvaluationIntegrationTest extends RestControllerAbstractTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private PolicyRepository policyRepository;

    @MockitoBean
    private AgentServiceClient agentServiceClient;

    private UUID persistPolicy(List<PolicyRule> rules) {
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
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

    private InternalEvaluateRequest request(UUID agentId, BigDecimal amount) {
        return InternalEvaluateRequest.builder()
                .agentId(agentId)
                .recipientAddress(SOME_RECIPIENT)
                .amount(amount)
                .requestedAt(Instant.now())
                .build();
    }

    @Test
    void shouldEvaluatePaymentEndToEnd() throws Exception {
        // given
        var agentId = persistPolicy(List.of(
                new PolicyRule.PerTransactionLimit(new BigDecimal("100.00")),
                new PolicyRule.DailyLimit(new BigDecimal("1000.00"))));

        // when

        // then
        mockMvc.perform(post("/api/v1/internal/policies/evaluate")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(agentId, new BigDecimal("30.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("APPROVED"))
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.agentId").value(agentId.toString()));
    }

    @Test
    void shouldRejectPaymentExceedingLimit() throws Exception {
        // given
        var agentId = persistPolicy(List.of(new PolicyRule.PerTransactionLimit(new BigDecimal("25.00"))));

        // when

        // then
        mockMvc.perform(post("/api/v1/internal/policies/evaluate")
                        .header("X-Service-Auth", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(agentId, new BigDecimal("30.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REJECTED"));
    }

    @Test
    void shouldRequireServiceAuth() throws Exception {
        // given
        var agentId = persistPolicy(List.of(new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"))));

        // when
        // then
        mockMvc.perform(post("/api/v1/internal/policies/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(agentId, new BigDecimal("30.00")))))
                .andExpect(status().isUnauthorized());
    }
}
