package com.arcpay.policy.policyengine.application.controller.mapper;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.FAIL_PER_TX;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.PASS_DAILY;
import static com.arcpay.policy.policyengine.test.fixtures.EvaluationFixtures.SOME_RECIPIENT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluationResponseMapperTest {

    private final EvaluationResponseMapper mapper = Mappers.getMapper(EvaluationResponseMapper.class);

    @Test
    void shouldMapRuleResultToApi() {
        // given
        var ruleResult = FAIL_PER_TX;

        // when
        var result = mapper.toApi(ruleResult);

        // then
        var expected = RuleResultResponse.builder()
                .ruleType("PER_TX_LIMIT")
                .verdict("FAIL")
                .limit(new BigDecimal("25.00"))
                .requested(new BigDecimal("30.00"))
                .message("Amount 30.00 exceeds per-transaction limit of 25.00")
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapEvaluationResultToApi() {
        // given
        var evaluationId = UuidCreator.getTimeOrderedEpoch();
        var evaluatedAt = Instant.parse("2026-01-07T10:00:00Z");
        var evalResult = PolicyEvaluationResult.builder()
                .evaluationId(evaluationId)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict(PolicyVerdict.REJECTED)
                .ruleResults(List.of(FAIL_PER_TX, PASS_DAILY))
                .requestedAmount(new BigDecimal("30.00"))
                .recipientAddress(SOME_RECIPIENT)
                .dryRun(true)
                .evaluatedAt(evaluatedAt)
                .durationMs(12)
                .build();

        // when
        var result = mapper.toApi(evalResult);

        // then
        var expected = PolicyEvaluationResponse.builder()
                .evaluationId(evaluationId)
                .agentId(SOME_AGENT_ID)
                .policyId(SOME_POLICY_ID)
                .verdict("REJECTED")
                .ruleResults(List.of(mapper.toApi(FAIL_PER_TX), mapper.toApi(PASS_DAILY)))
                .dryRun(true)
                .evaluatedAt(evaluatedAt)
                .durationMs(12)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
