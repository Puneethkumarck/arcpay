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

import static com.arcpay.policy.policyengine.domain.evaluation.EvaluatorTestSupport.contextWith;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApprovalThresholdEvaluatorTest {

    private final ApprovalThresholdEvaluator evaluator = new ApprovalThresholdEvaluator();

    @Nested
    class Evaluate {

        @Test
        void shouldPassWhenAmountUnderThreshold() {
            // given
            var rule = new PolicyRule.ApprovalThreshold(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("100.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("APPROVAL_THRESHOLD")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("500.00"))
                    .requested(new BigDecimal("100.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldPassWhenAmountEqualsThreshold() {
            // given
            var rule = new PolicyRule.ApprovalThreshold(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("500.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("APPROVAL_THRESHOLD")
                    .verdict(RuleVerdict.PASS)
                    .limit(new BigDecimal("500.00"))
                    .requested(new BigDecimal("500.00"))
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldRequireApprovalWhenAmountExceedsThreshold() {
            // given
            var rule = new PolicyRule.ApprovalThreshold(new BigDecimal("500.00"));
            var context = contextWith(new BigDecimal("750.00"), "0xrecipient");

            // when
            var result = evaluator.evaluate(rule, context);

            // then
            var expected = RuleEvaluationResult.builder()
                    .ruleType("APPROVAL_THRESHOLD")
                    .verdict(RuleVerdict.REQUIRES_APPROVAL)
                    .limit(new BigDecimal("500.00"))
                    .requested(new BigDecimal("750.00"))
                    .message("Amount 750.00 exceeds approval threshold of 500.00, owner approval required")
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
