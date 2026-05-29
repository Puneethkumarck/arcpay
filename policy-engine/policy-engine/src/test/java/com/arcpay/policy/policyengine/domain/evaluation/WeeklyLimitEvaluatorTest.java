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

class WeeklyLimitEvaluatorTest {

    private final WeeklyLimitEvaluator evaluator = new WeeklyLimitEvaluator();

    private SpendingSummary withWeekly(BigDecimal weeklyTotal) {
        return SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(weeklyTotal)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(0)
                .build();
    }

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenWeeklyTotalPlusAmountUnderLimit() {
            // given
            var rule = new PolicyRule.WeeklyLimit(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("100.00"), withWeekly(new BigDecimal("200.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("WEEKLY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("500.00"))
                    .current(new BigDecimal("200.00"))
                    .requested(new BigDecimal("100.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenWeeklyTotalPlusAmountEqualsLimit() {
            // given
            var rule = new PolicyRule.WeeklyLimit(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("300.00"), withWeekly(new BigDecimal("200.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("WEEKLY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("500.00"))
                    .current(new BigDecimal("200.00"))
                    .requested(new BigDecimal("300.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenWeeklyTotalPlusAmountExceedsLimit() {
            // given
            var rule = new PolicyRule.WeeklyLimit(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("301.00"), withWeekly(new BigDecimal("200.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("WEEKLY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("500.00"))
                    .current(new BigDecimal("200.00"))
                    .requested(new BigDecimal("301.00"))
                    .message("Weekly spending 200.00 + 301.00 would exceed limit of 500.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenWeeklyTotalAloneExceedsLimit() {
            // given
            var rule = new PolicyRule.WeeklyLimit(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("1.00"), withWeekly(new BigDecimal("600.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("WEEKLY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("500.00"))
                    .current(new BigDecimal("600.00"))
                    .requested(new BigDecimal("1.00"))
                    .message("Weekly spending 600.00 + 1.00 would exceed limit of 500.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
