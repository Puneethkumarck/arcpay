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

class MonthlyLimitEvaluatorTest {

    private final MonthlyLimitEvaluator evaluator = new MonthlyLimitEvaluator();

    private SpendingSummary withMonthly(BigDecimal monthlyTotal) {
        return SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(monthlyTotal)
                .velocityCount(0)
                .build();
    }

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenMonthlyTotalPlusAmountUnderLimit() {
            // given
            var rule = new PolicyRule.MonthlyLimit(new BigDecimal("2000.00"));
            var context = contextWith(new BigDecimal("500.00"), withMonthly(new BigDecimal("800.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("MONTHLY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("2000.00"))
                    .current(new BigDecimal("800.00"))
                    .requested(new BigDecimal("500.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenMonthlyTotalPlusAmountEqualsLimit() {
            // given
            var rule = new PolicyRule.MonthlyLimit(new BigDecimal("2000.00"));
            var context = contextWith(new BigDecimal("1200.00"), withMonthly(new BigDecimal("800.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("MONTHLY_LIMIT")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("2000.00"))
                    .current(new BigDecimal("800.00"))
                    .requested(new BigDecimal("1200.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenMonthlyTotalPlusAmountExceedsLimit() {
            // given
            var rule = new PolicyRule.MonthlyLimit(new BigDecimal("2000.00"));
            var context = contextWith(new BigDecimal("1300.00"), withMonthly(new BigDecimal("800.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("MONTHLY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("2000.00"))
                    .current(new BigDecimal("800.00"))
                    .requested(new BigDecimal("1300.00"))
                    .message("Monthly spending 800.00 + 1300.00 would exceed limit of 2000.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenMonthlyTotalAloneExceedsLimit() {
            // given
            var rule = new PolicyRule.MonthlyLimit(new BigDecimal("2000.00"));
            var context = contextWith(new BigDecimal("10.00"), withMonthly(new BigDecimal("2500.00")));

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("MONTHLY_LIMIT")
                    .verdict(RuleVerdict.FAIL)
                    .limit(new BigDecimal("2000.00"))
                    .current(new BigDecimal("2500.00"))
                    .requested(new BigDecimal("10.00"))
                    .message("Monthly spending 2500.00 + 10.00 would exceed limit of 2000.00")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
