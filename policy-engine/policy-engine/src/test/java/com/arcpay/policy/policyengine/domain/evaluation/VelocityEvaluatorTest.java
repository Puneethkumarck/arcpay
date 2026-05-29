package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

class VelocityEvaluatorTest {

    private final VelocityEvaluator evaluator = new VelocityEvaluator();

    private SpendingSummary withVelocityCount(int velocityCount) {
        return SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(velocityCount)
                .build();
    }

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenUnderMaxTransactions() {
            // given
            var rule = new PolicyRule.Velocity(50, 60);
            var context = contextWith(new BigDecimal("10.00"), withVelocityCount(12));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("VELOCITY")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("50"))
                    .current(new BigDecimal("12"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenVelocityCountIsZero() {
            // given
            var rule = new PolicyRule.Velocity(50, 60);
            var context = contextWith(new BigDecimal("10.00"), withVelocityCount(0));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("VELOCITY")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("50"))
                    .current(new BigDecimal("0"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenAtMaxTransactions() {
            // given
            var rule = new PolicyRule.Velocity(50, 60);
            var context = contextWith(new BigDecimal("10.00"), withVelocityCount(50));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("VELOCITY")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("50"))
                    .current(new BigDecimal("50"))
                    .message("Transaction count 50 in last 60 minutes would exceed limit of 50")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenOverMaxTransactions() {
            // given
            var rule = new PolicyRule.Velocity(50, 60);
            var context = contextWith(new BigDecimal("10.00"), withVelocityCount(73));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("VELOCITY")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("50"))
                    .current(new BigDecimal("73"))
                    .message("Transaction count 73 in last 60 minutes would exceed limit of 50")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
