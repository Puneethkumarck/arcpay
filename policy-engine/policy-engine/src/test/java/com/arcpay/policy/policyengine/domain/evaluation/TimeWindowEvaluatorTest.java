package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

class TimeWindowEvaluatorTest {

    private final TimeWindowEvaluator evaluator = new TimeWindowEvaluator();

    @Nested
    class Evaluate {

        @Test
        void shouldPassWithinTimeWindow() {
            // given — Wednesday 2026-01-07 at 10:00 UTC
            var rule = new PolicyRule.TimeWindow(9, 17, Set.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
            var context = contextWith(new BigDecimal("50.00"), "0xrecipient")
                    .toBuilder()
                    .requestedAt(Instant.parse("2026-01-07T10:00:00Z"))
                    .build();

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("TIME_WINDOW")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailOutsideAllowedHours() {
            // given — Wednesday 2026-01-07 at 20:00 UTC (outside 9-17)
            var rule = new PolicyRule.TimeWindow(9, 17, Set.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
            var context = contextWith(new BigDecimal("50.00"), "0xrecipient")
                    .toBuilder()
                    .requestedAt(Instant.parse("2026-01-07T20:00:00Z"))
                    .build();

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("TIME_WINDOW")
                    .verdict(RuleVerdict.FAIL)
                    .message("Transaction outside allowed time window (9-17 UTC, [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY])")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("message")
                    .isEqualTo(expected);
            assertThat(result.message()).startsWith("Transaction outside allowed time window");
        }

        @Test
        void shouldFailOnDisallowedDayOfWeek() {
            // given — Saturday 2026-01-10 at 10:00 UTC (weekend not allowed)
            var rule = new PolicyRule.TimeWindow(9, 17, Set.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));
            var context = contextWith(new BigDecimal("50.00"), "0xrecipient")
                    .toBuilder()
                    .requestedAt(Instant.parse("2026-01-10T10:00:00Z"))
                    .build();

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("TIME_WINDOW")
                    .verdict(RuleVerdict.FAIL)
                    .message("Transaction outside allowed time window (9-17 UTC, [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY])")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("message")
                    .isEqualTo(expected);
            assertThat(result.message()).startsWith("Transaction outside allowed time window");
        }

        @Test
        void shouldPassOnAllowedDayAndHour() {
            // given — Monday 2026-01-05 at 09:00 UTC (boundary start hour)
            var rule = new PolicyRule.TimeWindow(9, 17, Set.of(DayOfWeek.MONDAY));
            var context = contextWith(new BigDecimal("50.00"), "0xrecipient")
                    .toBuilder()
                    .requestedAt(Instant.parse("2026-01-05T09:00:00Z"))
                    .build();

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("TIME_WINDOW")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
