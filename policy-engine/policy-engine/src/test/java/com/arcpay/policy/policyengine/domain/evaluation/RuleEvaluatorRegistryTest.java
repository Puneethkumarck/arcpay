package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.exception.EvaluatorRegistrationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleEvaluatorRegistryTest {

    private static List<RuleEvaluator<?>> allEvaluators() {
        return List.of(
                new PerTransactionLimitEvaluator(),
                new RecipientAllowlistEvaluator(),
                new RecipientBlocklistEvaluator(),
                new TimeWindowEvaluator(),
                new ApprovalThresholdEvaluator(),
                new DailyLimitEvaluator(),
                new WeeklyLimitEvaluator(),
                new MonthlyLimitEvaluator(),
                new VelocityEvaluator(),
                new CooldownEvaluator());
    }

    @Nested
    class Discovery {

        @Test
        void shouldDiscoverAllTenEvaluators() {
            // given
            var evaluators = allEvaluators();

            // when
            var registry = new RuleEvaluatorRegistry(evaluators);

            // then — every permitted PolicyRule subtype resolves to an evaluator of the right type
            for (var subtype : PolicyRule.class.getPermittedSubclasses()) {
                @SuppressWarnings("unchecked")
                var ruleType = (Class<? extends PolicyRule>) subtype;
                assertThat(registry.getEvaluator(ruleType).supportedType()).isEqualTo(ruleType);
            }
            assertThat(PolicyRule.class.getPermittedSubclasses()).hasSize(10);
        }
    }

    @Nested
    class Dispatch {

        @Test
        void shouldReturnCorrectEvaluatorForEachRuleType() {
            // given
            var registry = new RuleEvaluatorRegistry(allEvaluators());

            // when / then
            assertThat(registry.getEvaluator(PolicyRule.PerTransactionLimit.class))
                    .isInstanceOf(PerTransactionLimitEvaluator.class);
            assertThat(registry.getEvaluator(PolicyRule.DailyLimit.class))
                    .isInstanceOf(DailyLimitEvaluator.class);
            assertThat(registry.getEvaluator(PolicyRule.Velocity.class))
                    .isInstanceOf(VelocityEvaluator.class);
            assertThat(registry.getEvaluator(PolicyRule.ApprovalThreshold.class))
                    .isInstanceOf(ApprovalThresholdEvaluator.class);
        }
    }

    @Nested
    class FailFast {

        @Test
        void shouldFailWhenRuleTypeHasNoEvaluator() {
            // given — missing the PerTransactionLimit evaluator
            var incomplete = allEvaluators().stream()
                    .filter(e -> e.supportedType() != PolicyRule.PerTransactionLimit.class)
                    .toList();

            // when / then
            assertThatThrownBy(() -> new RuleEvaluatorRegistry(incomplete))
                    .isInstanceOf(EvaluatorRegistrationException.class)
                    .hasMessageContaining("PerTransactionLimit");
        }

        @Test
        void shouldFailWhenTwoEvaluatorsClaimSameRuleType() {
            // given
            var duplicated = new java.util.ArrayList<RuleEvaluator<?>>(allEvaluators());
            duplicated.add(new DailyLimitEvaluator());

            // when / then
            assertThatThrownBy(() -> new RuleEvaluatorRegistry(duplicated))
                    .isInstanceOf(EvaluatorRegistrationException.class)
                    .hasMessageContaining("Multiple evaluators");
        }
    }
}
