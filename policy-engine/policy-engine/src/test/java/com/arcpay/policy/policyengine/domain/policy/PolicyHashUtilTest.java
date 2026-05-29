package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        @Test
        @SneakyThrows
        void shouldProduceSameHashRegardlessOfAddressSetOrder() {
            // given — same logical addresses inserted in different iteration orders
            Set<String> orderA = new LinkedHashSet<>();
            orderA.add("0xAAAA000000000000000000000000000000000001");
            orderA.add("0xBBBB000000000000000000000000000000000002");
            orderA.add("0xCCCC000000000000000000000000000000000003");

            Set<String> orderB = new LinkedHashSet<>();
            orderB.add("0xCCCC000000000000000000000000000000000003");
            orderB.add("0xAAAA000000000000000000000000000000000001");
            orderB.add("0xBBBB000000000000000000000000000000000002");

            var rulesA = List.<PolicyRule>of(new PolicyRule.RecipientAllowlist(orderA));
            var rulesB = List.<PolicyRule>of(new PolicyRule.RecipientAllowlist(orderB));

            // when
            var hashA = PolicyHashUtil.computePolicyHash(rulesA);
            var hashB = PolicyHashUtil.computePolicyHash(rulesB);

            // then
            assertThat(hashA).isEqualTo(hashB);
        }

        @Test
        @SneakyThrows
        void shouldProduceSameHashRegardlessOfBlocklistSetOrder() {
            // given — same logical addresses, mixed casing, different iteration orders
            Set<String> orderA = new LinkedHashSet<>();
            orderA.add("0xABCDEF0000000000000000000000000000000001");
            orderA.add("0x1234560000000000000000000000000000000002");

            Set<String> orderB = new LinkedHashSet<>();
            orderB.add("0x1234560000000000000000000000000000000002");
            orderB.add("0xabcdef0000000000000000000000000000000001");

            var rulesA = List.<PolicyRule>of(new PolicyRule.RecipientBlocklist(orderA));
            var rulesB = List.<PolicyRule>of(new PolicyRule.RecipientBlocklist(orderB));

            // when
            var hashA = PolicyHashUtil.computePolicyHash(rulesA);
            var hashB = PolicyHashUtil.computePolicyHash(rulesB);

            // then
            assertThat(hashA).isEqualTo(hashB);
        }

        @Test
        @SneakyThrows
        void shouldProduceSameHashRegardlessOfDaysOfWeekSetOrder() {
            // given — same days inserted in different iteration orders
            Set<DayOfWeek> orderA = new LinkedHashSet<>();
            orderA.add(DayOfWeek.WEDNESDAY);
            orderA.add(DayOfWeek.MONDAY);
            orderA.add(DayOfWeek.FRIDAY);

            Set<DayOfWeek> orderB = new LinkedHashSet<>();
            orderB.add(DayOfWeek.FRIDAY);
            orderB.add(DayOfWeek.WEDNESDAY);
            orderB.add(DayOfWeek.MONDAY);

            var rulesA = List.<PolicyRule>of(new PolicyRule.TimeWindow(9, 17, orderA));
            var rulesB = List.<PolicyRule>of(new PolicyRule.TimeWindow(9, 17, orderB));

            // when
            var hashA = PolicyHashUtil.computePolicyHash(rulesA);
            var hashB = PolicyHashUtil.computePolicyHash(rulesB);

            // then
            assertThat(hashA).isEqualTo(hashB);
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

        @Test
        @SneakyThrows
        void shouldProduceDifferentHashForSameAmountButDifferentRuleType() {
            // given — same amount, different limit types must not collide (type discriminator)
            var daily = List.<PolicyRule>of(new PolicyRule.DailyLimit(new BigDecimal("1000.00")));
            var weekly = List.<PolicyRule>of(new PolicyRule.WeeklyLimit(new BigDecimal("1000.00")));

            // when
            var dailyHash = PolicyHashUtil.computePolicyHash(daily);
            var weeklyHash = PolicyHashUtil.computePolicyHash(weekly);

            // then
            assertThat(dailyHash).isNotEqualTo(weeklyHash);
        }

        @Test
        @SneakyThrows
        void shouldDistinguishHighScaleAmountsBeyondDoublePrecision() {
            // given — amounts differing only in the 18th significant digit; serialized as JSON
            // numbers they would round to the same IEEE-754 double and collide
            var rulesA = List.<PolicyRule>of(new PolicyRule.DailyLimit(new BigDecimal("999999999999.000001")));
            var rulesB = List.<PolicyRule>of(new PolicyRule.DailyLimit(new BigDecimal("999999999999.000002")));

            // when
            var hashA = PolicyHashUtil.computePolicyHash(rulesA);
            var hashB = PolicyHashUtil.computePolicyHash(rulesB);

            // then
            assertThat(hashA).isNotEqualTo(hashB);
        }
    }

    @Nested
    class AmountScale {

        @Test
        @SneakyThrows
        void shouldProduceSameHashForAmountsDifferingOnlyInTrailingZeros() {
            // given — 1000.00 and 1000 are the same monetary amount and must hash identically
            var rulesScaled = List.<PolicyRule>of(new PolicyRule.DailyLimit(new BigDecimal("1000.00")));
            var rulesPlain = List.<PolicyRule>of(new PolicyRule.DailyLimit(new BigDecimal("1000")));

            // when
            var scaledHash = PolicyHashUtil.computePolicyHash(rulesScaled);
            var plainHash = PolicyHashUtil.computePolicyHash(rulesPlain);

            // then
            assertThat(scaledHash).isEqualTo(plainHash);
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
