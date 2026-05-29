package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

class RecipientAllowlistEvaluatorTest {

    private final RecipientAllowlistEvaluator evaluator = new RecipientAllowlistEvaluator();

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenRecipientInAllowlist() {
            // given
            var rule = new PolicyRule.RecipientAllowlist(Set.of("0xabc123", "0xdef456"));
            var context = contextWith(new BigDecimal("50.00"), "0xabc123");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_ALLOWLIST")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenRecipientNotInAllowlist() {
            // given
            var rule = new PolicyRule.RecipientAllowlist(Set.of("0xabc123", "0xdef456"));
            var context = contextWith(new BigDecimal("50.00"), "0x999999");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_ALLOWLIST")
                    .verdict(RuleVerdict.FAIL)
                    .message("Recipient 0x999999 is not in the allowlist")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldBeCaseInsensitive() {
            // given
            var rule = new PolicyRule.RecipientAllowlist(Set.of("0xABC123"));
            var context = contextWith(new BigDecimal("50.00"), "0xabc123");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_ALLOWLIST")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
