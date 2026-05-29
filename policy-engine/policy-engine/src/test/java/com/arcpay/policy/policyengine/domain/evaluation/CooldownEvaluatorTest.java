package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextAt;
import static org.assertj.core.api.Assertions.assertThat;

class CooldownEvaluatorTest {

    private final CooldownEvaluator evaluator = new CooldownEvaluator();

    private static final Instant SOME_REQUESTED_AT = Instant.parse("2026-01-07T10:00:00Z");

    private SpendingSummary withLastTransactionAt(Instant lastTransactionAt) {
        return SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(0)
                .lastTransactionAt(lastTransactionAt)
                .build();
    }

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenNoPriorTransactions() {
            // given
            var rule = new PolicyRule.Cooldown(60);
            var context = contextAt(new BigDecimal("10.00"), SOME_REQUESTED_AT, withLastTransactionAt(null));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("COOLDOWN")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenCooldownElapsed() {
            // given
            var rule = new PolicyRule.Cooldown(60);
            var lastTransactionAt = SOME_REQUESTED_AT.minusSeconds(120);
            var context = contextAt(new BigDecimal("10.00"), SOME_REQUESTED_AT, withLastTransactionAt(lastTransactionAt));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("COOLDOWN")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenExactlyCooldownSecondsElapsed() {
            // given
            var rule = new PolicyRule.Cooldown(60);
            var lastTransactionAt = SOME_REQUESTED_AT.minusSeconds(60);
            var context = contextAt(new BigDecimal("10.00"), SOME_REQUESTED_AT, withLastTransactionAt(lastTransactionAt));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("COOLDOWN")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenCooldownNotElapsed() {
            // given
            var rule = new PolicyRule.Cooldown(60);
            var lastTransactionAt = SOME_REQUESTED_AT.minusSeconds(30);
            var context = contextAt(new BigDecimal("10.00"), SOME_REQUESTED_AT, withLastTransactionAt(lastTransactionAt));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("COOLDOWN")
                    .verdict(RuleVerdict.FAIL)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("message")
                    .isEqualTo(expected);
            assertThat(result.message()).startsWith("Cooldown period not elapsed. Last transaction at");
        }
    }
}
