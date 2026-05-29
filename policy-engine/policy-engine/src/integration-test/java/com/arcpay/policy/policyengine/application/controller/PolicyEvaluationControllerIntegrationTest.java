package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.test.RestControllerAbstractTest;
import tools.jackson.databind.json.JsonMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.FAIL_PER_TX;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.SOME_RECIPIENT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_AGENT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_OWNED_BY_OTHER;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUSPENDED_AGENT;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyEvaluationControllerIntegrationTest extends RestControllerAbstractTest {

    private static final BigDecimal AMOUNT = new BigDecimal("30.00");
    private static final String EVALUATE_BODY = """
            {
              "agentId": "019576a0-0000-7000-8000-000000000002",
              "recipientAddress": "0x1234567890abcdef1234567890abcdef12345678",
              "amount": "30.00"
            }
            """;

    @MockitoBean
    private AgentServiceClient agentServiceClient;

    @MockitoBean
    private PolicyEvaluationService policyEvaluationService;

    @Autowired
    private JsonMapper jsonMapper;

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private PolicyEvaluationResult rejectedResult(UUID evaluationId, Instant evaluatedAt) {
        return PolicyEvaluationResult.builder()
                .evaluationId(evaluationId)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(PolicyVerdict.REJECTED)
                .ruleResults(List.of(FAIL_PER_TX))
                .requestedAmount(AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(true)
                .evaluatedAt(evaluatedAt)
                .durationMs(12)
                .build();
    }

    @Test
    void shouldEvaluateDryRunAndReturnResult() throws Exception {
        // given
        var evaluationId = UuidCreator.getTimeOrderedEpoch();
        var evaluatedAt = Instant.parse("2026-01-07T10:00:00Z");
        var result = rejectedResult(evaluationId, evaluatedAt);
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(dryRunEvaluate()).willReturn(result);

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PolicyEvaluationResponse.class);
        var expected = PolicyEvaluationResponse.builder()
                .evaluationId(evaluationId)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict("REJECTED")
                .ruleResults(List.of(RuleResultResponse.builder()
                        .ruleType("PER_TX_LIMIT")
                        .verdict("FAIL")
                        .limit(new BigDecimal("25.00"))
                        .requested(AMOUNT)
                        .message("Amount 30.00 exceeds per-transaction limit of 25.00")
                        .build()))
                .dryRun(true)
                .evaluatedAt(evaluatedAt)
                .durationMs(12)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn403WhenAgentNotOwned() throws Exception {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_AGENT_OWNED_BY_OTHER));

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0006");
    }

    @Test
    void shouldReturn404WhenAgentNotFound() throws Exception {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0005");
    }

    @Test
    void shouldReturn422WhenAgentNotActive() throws Exception {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_SUSPENDED_AGENT));

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0004");
    }

    @Test
    void shouldReturn404WhenNoActivePolicy() throws Exception {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(dryRunEvaluate()).willThrow(new PolicyNotFoundException(SOME_AGENT_ID, "no policy configured"));

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0001");
    }

    @Test
    void shouldReturn400WhenAmountTooSmall() throws Exception {
        // given — amount below @DecimalMin

        // when
        var response = mockMvc.perform(post("/api/v1/policies/evaluate")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "019576a0-0000-7000-8000-000000000002",
                                  "recipientAddress": "0x1234567890abcdef1234567890abcdef12345678",
                                  "amount": "0"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0002");
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/policies/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EVALUATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    private PolicyEvaluationResult dryRunEvaluate() {
        return policyEvaluationService.evaluate(
                eqIgnoringTimestamps(SOME_AGENT_ID),
                eqIgnoringTimestamps(SOME_RECIPIENT),
                eqIgnoringTimestamps(AMOUNT),
                eqIgnoringTimestamps(Instant.now()),
                booleanThat(Boolean.TRUE::equals));
    }
}
