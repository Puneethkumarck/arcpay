package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

class PerTransactionLimitEvaluatorTest {

    private final PerTransactionLimitEvaluator evaluator = new PerTransactionLimitEvaluator();

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenAmountUnderLimit() {
            // given
            var rule = new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("50.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("PER_TX_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("100.00"))
                    .requested(new BigDecimal("50.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenAmountEqualsLimit() {
            // given
            var rule = new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("100.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("PER_TX_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("100.00"))
                    .requested(new BigDecimal("100.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenAmountExceedsLimit() {
            // given
            var rule = new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("150.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("PER_TX_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("100.00"))
                    .requested(new BigDecimal("150.00"))
                    .message("Amount 150.00 exceeds per-transaction limit of 100.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
