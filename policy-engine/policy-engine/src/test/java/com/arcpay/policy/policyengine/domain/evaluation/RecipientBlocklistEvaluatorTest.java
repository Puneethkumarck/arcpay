package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RecipientBlocklistEvaluatorTest {

    private final RecipientBlocklistEvaluator evaluator = new RecipientBlocklistEvaluator();

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenRecipientNotBlocklisted() {
            // given
            var rule = new PolicyRule.RecipientBlocklist(Set.of("0xbad111", "0xbad222"));
            var context = contextWith(new BigDecimal("50.00"), "0xgood999");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_BLOCKLIST")
                    .verdict(RuleVerdict.PASS)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldFailWhenRecipientBlocklisted() {
            // given
            var rule = new PolicyRule.RecipientBlocklist(Set.of("0xbad111", "0xbad222"));
            var context = contextWith(new BigDecimal("50.00"), "0xbad111");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_BLOCKLIST")
                    .verdict(RuleVerdict.FAIL)
                    .message("Recipient 0xbad111 is blocklisted")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldBeCaseInsensitive() {
            // given
            var rule = new PolicyRule.RecipientBlocklist(Set.of("0xBAD111"));
            var context = contextWith(new BigDecimal("50.00"), "0xbad111");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("RECIPIENT_BLOCKLIST")
                    .verdict(RuleVerdict.FAIL)
                    .message("Recipient 0xbad111 is blocklisted")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
