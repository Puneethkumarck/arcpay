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

class DailyLimitEvaluatorTest {

    private final DailyLimitEvaluator evaluator = new DailyLimitEvaluator();

    private SpendingSummary withDaily(BigDecimal dailyTotal) {
        return SpendingSummary.builder()
                .dailyTotal(dailyTotal)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(0)
                .build();
    }

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenDailyTotalPlusAmountUnderLimit() {
            // given
            var rule = new PolicyRule.DailyLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("30.00"), withDaily(new BigDecimal("45.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("DAILY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("100.00"))
                    .current(new BigDecimal("45.00"))
                    .requested(new BigDecimal("30.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenDailyTotalPlusAmountEqualsLimit() {
            // given
            var rule = new PolicyRule.DailyLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("55.00"), withDaily(new BigDecimal("45.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("DAILY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("100.00"))
                    .current(new BigDecimal("45.00"))
                    .requested(new BigDecimal("55.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenDailyTotalPlusAmountExceedsLimit() {
            // given
            var rule = new PolicyRule.DailyLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("60.00"), withDaily(new BigDecimal("45.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("DAILY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("100.00"))
                    .current(new BigDecimal("45.00"))
                    .requested(new BigDecimal("60.00"))
                    .message("Daily spending 45.00 + 60.00 would exceed limit of 100.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenDailyTotalAloneExceedsLimit() {
            // given
            var rule = new PolicyRule.DailyLimit(new BigDecimal("100.00"));
            var context = contextWith(new BigDecimal("5.00"), withDaily(new BigDecimal("120.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("DAILY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("100.00"))
                    .current(new BigDecimal("120.00"))
                    .requested(new BigDecimal("5.00"))
                    .message("Daily spending 120.00 + 5.00 would exceed limit of 100.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
