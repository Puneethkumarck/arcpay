package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyHashUtilTest {

    @Nested
    class Determinism {

        @Test
        @SneakyThrows
        void shouldComputeDeterministicHash() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00")),
                    new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"))
            );

            // when
            var hash1 = PolicyHashUtil.computePolicyHash(rules);
            var hash2 = PolicyHashUtil.computePolicyHash(rules);

            // then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @SneakyThrows
        void shouldProduceSameHashRegardlessOfRuleOrder() {
            // given
            var rulesOrder1 = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00")),
                    new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"))
            );
            var rulesOrder2 = List.<PolicyRule>of(
                    new PolicyRule.PerTransactionLimit(new BigDecimal("100.00")),
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00"))
            );

            // when
            var hash1 = PolicyHashUtil.computePolicyHash(rulesOrder1);
            var hash2 = PolicyHashUtil.computePolicyHash(rulesOrder2);

            // then
            assertThat(hash1).isEqualTo(hash2);
        }
    }

    @Nested
    class Uniqueness {

        @Test
        @SneakyThrows
        void shouldProduceDifferentHashForDifferentRules() {
            // given
            var rules1 = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00"))
            );
            var rules2 = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("2000.00"))
            );

            // when
            var hash1 = PolicyHashUtil.computePolicyHash(rules1);
            var hash2 = PolicyHashUtil.computePolicyHash(rules2);

            // then
            assertThat(hash1).isNotEqualTo(hash2);
        }
    }

    @Nested
    class Format {

        @Test
        @SneakyThrows
        void shouldReturnHexPrefixedHash() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00"))
            );

            // when
            var hash = PolicyHashUtil.computePolicyHash(rules);

            // then
            assertThat(hash).startsWith("0x");
            assertThat(hash).hasSize(66); // 0x + 64 hex chars
        }
    }

    @Nested
    class CanonicalJson {

        @Test
        @SneakyThrows
        void shouldHandleCanonicalJsonOrdering() {
            // given — different field ordering in source doesn't matter since Jackson controls serialization,
            // but canonical JSON ensures consistent key ordering across implementations
            var rules1 = List.<PolicyRule>of(
                    new PolicyRule.Cooldown(300),
                    new PolicyRule.ApprovalThreshold(new BigDecimal("10000.00"))
            );
            var rules2 = List.<PolicyRule>of(
                    new PolicyRule.ApprovalThreshold(new BigDecimal("10000.00")),
                    new PolicyRule.Cooldown(300)
            );

            // when
            var hash1 = PolicyHashUtil.computePolicyHash(rules1);
            var hash2 = PolicyHashUtil.computePolicyHash(rules2);

            // then
            assertThat(hash1).isEqualTo(hash2);
        }
    }
}
