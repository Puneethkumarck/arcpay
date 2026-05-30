package com.arcpay.payment.paymentexecution.infrastructure.client.policy;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyResultMapperTest {

    private final PolicyResultMapper mapper = Mappers.getMapper(PolicyResultMapper.class);

    @Test
    void shouldMapVerdictAndCountRules() {
        // given
        var response = PolicyEvaluationResponse.builder()
                .evaluationId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .verdict("APPROVED")
                .ruleResults(List.of(
                        RuleResultResponse.builder().build(),
                        RuleResultResponse.builder().build()))
                .dryRun(false)
                .evaluatedAt(Instant.parse("2026-05-29T10:00:00Z"))
                .durationMs(12)
                .build();

        // when
        var result = mapper.toDomain(response);

        // then
        var expected = PolicyResult.builder()
                .verdict("APPROVED")
                .rulesEvaluated(2)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldDefaultRuleCountToZeroWhenNull() {
        // given
        var response = PolicyEvaluationResponse.builder()
                .verdict("REJECTED")
                .ruleResults(null)
                .build();

        // when
        var result = mapper.toDomain(response);

        // then
        var expected = PolicyResult.builder()
                .verdict("REJECTED")
                .rulesEvaluated(0)
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
