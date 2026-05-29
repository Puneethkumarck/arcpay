package com.arcpay.policy.policyengine.infrastructure.db.evaluation;

import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import com.arcpay.policy.policyengine.domain.port.PolicyEvaluationRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Transactional
class PolicyEvaluationRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private PolicyEvaluationRepository policyEvaluationRepository;

    @Autowired
    private PolicyEvaluationJpaRepository policyEvaluationJpaRepository;

    @Test
    void shouldSaveAndReadBackEvaluation() {
        // given
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var evaluationId = UUID.randomUUID();
        var agentId = UUID.randomUUID();
        var policyId = UUID.randomUUID();
        var evaluation = PolicyEvaluationResult.builder()
                .evaluationId(evaluationId)
                .agentId(agentId)
                .policyId(policyId)
                .verdict(PolicyVerdict.APPROVED)
                .ruleResults(List.of(
                        RuleEvaluationResult.builder()
                                .ruleType("DAILY_LIMIT")
                                .verdict(RuleVerdict.PASS)
                                .limit(new BigDecimal("1000.00"))
                                .current(new BigDecimal("100.00"))
                                .requested(new BigDecimal("50.00"))
                                .message("Within daily limit")
                                .build()
                ))
                .requestedAmount(new BigDecimal("50.000000"))
                .recipientAddress("0x1234567890abcdef1234567890abcdef12345678")
                .dryRun(false)
                .evaluatedAt(now)
                .durationMs(42)
                .build();

        // when
        policyEvaluationRepository.save(evaluation);
        var loaded = policyEvaluationJpaRepository.findById(evaluationId).orElseThrow();

        // then
        var expected = PolicyEvaluationEntity.builder()
                .evaluationId(evaluationId)
                .agentId(agentId)
                .policyId(policyId)
                .verdict(PolicyVerdict.APPROVED)
                .ruleResults(loaded.getRuleResults())
                .requestedAmount(new BigDecimal("50.000000"))
                .recipientAddress("0x1234567890abcdef1234567890abcdef12345678")
                .durationMs(42)
                .dryRun(false)
                .evaluatedAt(now)
                .build();
        assertThat(loaded).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldDeleteOldEvaluations() {
        // given
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var evaluation = PolicyEvaluationResult.builder()
                .evaluationId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .verdict(PolicyVerdict.APPROVED)
                .ruleResults(List.of(
                        RuleEvaluationResult.builder()
                                .ruleType("DAILY_LIMIT")
                                .verdict(RuleVerdict.PASS)
                                .limit(new BigDecimal("1000.00"))
                                .current(new BigDecimal("100.00"))
                                .requested(new BigDecimal("50.00"))
                                .message("Within daily limit")
                                .build()
                ))
                .requestedAmount(new BigDecimal("50.000000"))
                .recipientAddress("0x1234567890abcdef1234567890abcdef12345678")
                .dryRun(false)
                .evaluatedAt(now.minus(2, ChronoUnit.DAYS))
                .durationMs(42)
                .build();
        policyEvaluationRepository.save(evaluation);

        // when
        var cutoff = now.minus(1, ChronoUnit.DAYS);

        // then
        assertThatCode(() -> policyEvaluationRepository.deleteOlderThan(cutoff))
                .doesNotThrowAnyException();
    }
}
