package com.arcpay.policy.policyengine.application.controller.internal;

import com.arcpay.policy.policyengine.api.model.InternalEvaluateRequest;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import com.arcpay.policy.policyengine.application.controller.internal.mapper.EvaluationResponseMapper;
import com.arcpay.policy.policyengine.domain.evaluation.PolicyEvaluationService;
import com.arcpay.policy.policyengine.domain.exception.PolicyHashMismatchException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalPolicyEvaluationControllerTest {

    private static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000010");
    private static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000020");
    private static final UUID SOME_EVALUATION_ID = UUID.fromString("019576a0-0000-7000-8000-000000000030");
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");
    private static final Instant SOME_REQUESTED_AT = Instant.parse("2026-01-07T10:00:00Z");
    private static final Instant SOME_EVALUATED_AT = Instant.parse("2026-01-07T10:00:01Z");

    @Mock
    private PolicyEvaluationService policyEvaluationService;

    private final EvaluationResponseMapper evaluationResponseMapper =
            Mappers.getMapper(EvaluationResponseMapper.class);

    private InternalPolicyEvaluationController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalPolicyEvaluationController(policyEvaluationService, evaluationResponseMapper);
    }

    private InternalEvaluateRequest someRequest() {
        return InternalEvaluateRequest.builder()
                .agentId(SOME_AGENT_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .requestedAt(SOME_REQUESTED_AT)
                .build();
    }

    @Test
    void shouldEvaluateAndReturnResponse() {
        // given
        var ruleResult = RuleEvaluationResult.builder()
                .ruleType("PER_TX_LIMIT")
                .verdict(RuleVerdict.PASS)
                .limit(new BigDecimal("100.00"))
                .requested(SOME_AMOUNT)
                .build();
        var result = PolicyEvaluationResult.builder()
                .evaluationId(SOME_EVALUATION_ID)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(PolicyVerdict.APPROVED)
                .ruleResults(List.of(ruleResult))
                .requestedAmount(SOME_AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(false)
                .evaluatedAt(SOME_EVALUATED_AT)
                .durationMs(12)
                .build();
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, false))
                .willReturn(result);

        // when
        var response = controller.evaluate(someRequest());

        // then
        var expected = PolicyEvaluationResponse.builder()
                .evaluationId(SOME_EVALUATION_ID)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict("APPROVED")
                .ruleResults(List.of(RuleResultResponse.builder()
                        .ruleType("PER_TX_LIMIT")
                        .verdict("PASS")
                        .limit(new BigDecimal("100.00"))
                        .requested(SOME_AMOUNT)
                        .build()))
                .dryRun(false)
                .evaluatedAt(SOME_EVALUATED_AT)
                .durationMs(12)
                .build();
        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldEvaluateInRealModeWithDryRunFalse() {
        // given
        var result = PolicyEvaluationResult.builder()
                .evaluationId(SOME_EVALUATION_ID)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(PolicyVerdict.REJECTED)
                .ruleResults(List.of(RuleEvaluationResult.builder()
                        .ruleType("PER_TX_LIMIT")
                        .verdict(RuleVerdict.FAIL)
                        .build()))
                .requestedAmount(SOME_AMOUNT)
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(false)
                .evaluatedAt(SOME_EVALUATED_AT)
                .durationMs(5)
                .build();
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, false))
                .willReturn(result);

        // when
        var response = controller.evaluate(someRequest());

        // then
        assertThat(response.verdict()).isEqualTo("REJECTED");
        assertThat(response.dryRun()).isFalse();
    }

    @Test
    void shouldPropagatePolicyNotFound() {
        // given
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, false))
                .willThrow(new PolicyNotFoundException(SOME_AGENT_ID, "no policy configured"));

        // when / then
        assertThatThrownBy(() -> controller.evaluate(someRequest()))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void shouldPropagatePolicyHashMismatch() {
        // given
        given(policyEvaluationService.evaluate(
                SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT, SOME_REQUESTED_AT, false))
                .willThrow(new PolicyHashMismatchException(SOME_AGENT_ID, "0xexpected", "0xactual"));

        // when / then
        assertThatThrownBy(() -> controller.evaluate(someRequest()))
                .isInstanceOf(PolicyHashMismatchException.class);
    }
}
