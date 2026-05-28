package com.arcpay.policy.policyengine.domain.model;

import com.arcpay.policy.policyengine.api.PolicyRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_CREATED_AT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_RULES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyTest {

    @Nested
    class Construction {

        @Test
        void shouldCreatePolicyWithValidFields() {
            // given / when
            var policy = SOME_ACTIVE_POLICY;

            // then
            var expected = Policy.builder()
                    .policyId(SOME_POLICY_ID)
                    .agentId(SOME_AGENT_ID)
                    .ownerId(SOME_OWNER_ID)
                    .version(1)
                    .rules(SOME_RULES)
                    .policyHash(SOME_POLICY_HASH)
                    .status(PolicyStatus.ACTIVE)
                    .createdAt(SOME_CREATED_AT)
                    .updatedAt(SOME_CREATED_AT)
                    .build();

            assertThat(policy)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        void shouldRejectNullPolicyId() {
            // when/then
            assertThatThrownBy(() -> SOME_ACTIVE_POLICY.toBuilder().policyId(null).build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("policyId must not be null");
        }

        @Test
        void shouldRejectNullAgentId() {
            // when/then
            assertThatThrownBy(() -> SOME_ACTIVE_POLICY.toBuilder().agentId(null).build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("agentId must not be null");
        }

        @Test
        void shouldRejectNullRules() {
            // when/then
            assertThatThrownBy(() -> SOME_ACTIVE_POLICY.toBuilder().rules(null).build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("rules must not be null");
        }

        @Test
        void shouldRejectEmptyRules() {
            // when/then
            assertThatThrownBy(() -> SOME_ACTIVE_POLICY.toBuilder().rules(List.of()).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("rules must not be empty");
        }

        @Test
        void shouldDefensivelyCopyRules() {
            // given
            var mutableRules = new java.util.ArrayList<PolicyRule>(List.of(
                    new PolicyRule.DailyLimit(new BigDecimal("500.00"))
            ));

            // when
            var policy = SOME_ACTIVE_POLICY.toBuilder().rules(mutableRules).build();
            mutableRules.add(new PolicyRule.WeeklyLimit(new BigDecimal("2000.00")));

            // then
            assertThat(policy.rules()).hasSize(1);
        }
    }

    @Nested
    class Supersede {

        @Test
        void shouldSupersedePolicy() {
            // given
            var policy = SOME_ACTIVE_POLICY;

            // when
            var result = policy.supersede();

            // then
            var expected = policy.toBuilder()
                    .status(PolicyStatus.SUPERSEDED)
                    .build();

            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .isEqualTo(expected);
            assertThat(result.updatedAt()).isAfterOrEqualTo(policy.updatedAt());
        }

        @Test
        void shouldReturnNewInstanceOnSupersede() {
            // given
            var policy = SOME_ACTIVE_POLICY;

            // when
            var result = policy.supersede();

            // then
            assertThat(result).isNotSameAs(policy);
            assertThat(policy.status()).isEqualTo(PolicyStatus.ACTIVE);
        }
    }
}
