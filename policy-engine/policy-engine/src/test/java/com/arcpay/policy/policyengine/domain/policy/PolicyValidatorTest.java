package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PolicyValidatorTest {

    @InjectMocks
    private PolicyValidator policyValidator;

    @Nested
    class ValidPolicies {

        @Test
        void shouldAcceptValidPolicyWithMultipleRules() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00")),
                    new PolicyRule.WeeklyLimit(new BigDecimal("5000.00")),
                    new PolicyRule.MonthlyLimit(new BigDecimal("20000.00")),
                    new PolicyRule.PerTransactionLimit(new BigDecimal("500.00"))
            );

            // when / then
            assertThatCode(() -> policyValidator.validate(rules))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldAcceptDailyLimitEqualToWeeklyLimit() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00")),
                    new PolicyRule.WeeklyLimit(new BigDecimal("1000.00"))
            );

            // when / then
            assertThatCode(() -> policyValidator.validate(rules))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class EmptyRules {

        @Test
        void shouldRejectEmptyRulesList() {
            // given
            var rules = List.<PolicyRule>of();

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("must not be empty");
        }
    }

    @Nested
    class DuplicateRuleTypes {

        @Test
        void shouldRejectDuplicateRuleTypes() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("1000.00")),
                    new PolicyRule.DailyLimit(new BigDecimal("2000.00"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("Duplicate rule type");
        }
    }

    @Nested
    class LimitHierarchy {

        @Test
        void shouldRejectDailyLimitGreaterThanWeeklyLimit() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("5000.00")),
                    new PolicyRule.WeeklyLimit(new BigDecimal("1000.00"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("DAILY_LIMIT amount must be <= WEEKLY_LIMIT amount");
        }

        @Test
        void shouldRejectWeeklyLimitGreaterThanMonthlyLimit() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.WeeklyLimit(new BigDecimal("25000.00")),
                    new PolicyRule.MonthlyLimit(new BigDecimal("20000.00"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("WEEKLY_LIMIT amount must be <= MONTHLY_LIMIT amount");
        }
    }

    @Nested
    class AddressOverlap {

        @Test
        void shouldRejectAddressInBothAllowlistAndBlocklist() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.RecipientAllowlist(Set.of("0x1234567890abcdef1234567890abcdef12345678")),
                    new PolicyRule.RecipientBlocklist(Set.of("0x1234567890abcdef1234567890abcdef12345678"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("Address overlap between allowlist and blocklist");
        }

        @Test
        void shouldRejectOverlappingAddressesCaseInsensitive() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.RecipientAllowlist(Set.of("0xABCDEF1234567890ABCDEF1234567890ABCDEF12")),
                    new PolicyRule.RecipientBlocklist(Set.of("0xabcdef1234567890abcdef1234567890abcdef12"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("Address overlap between allowlist and blocklist");
        }
    }

    @Nested
    class AmountValidation {

        @Test
        void shouldRejectNegativeAmount() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.DailyLimit(new BigDecimal("-100.00"))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("amount must be greater than 0");
        }

        @Test
        void shouldRejectZeroAmount() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.PerTransactionLimit(BigDecimal.ZERO)
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("amount must be greater than 0");
        }
    }

    @Nested
    class AddressSetValidation {

        @Test
        void shouldRejectEmptyAddressSet() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.RecipientAllowlist(Set.of())
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("addresses must not be empty");
        }

        @Test
        void shouldRejectAddressSetOver100Entries() {
            // given
            var addresses = IntStream.rangeClosed(1, 101)
                    .mapToObj(i -> "0x" + String.format("%040d", i))
                    .collect(Collectors.toSet());
            var rules = List.<PolicyRule>of(
                    new PolicyRule.RecipientBlocklist(addresses)
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("must not exceed 100 entries");
        }
    }

    @Nested
    class TimeWindowValidation {

        @Test
        void shouldRejectStartHourGreaterThanEndHour() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.TimeWindow(17, 9, Set.of(DayOfWeek.MONDAY))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("startHour must be less than endHour");
        }

        @Test
        void shouldRejectStartHourEqualsEndHour() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.TimeWindow(9, 9, Set.of(DayOfWeek.MONDAY))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("startHour must be less than endHour");
        }

        @Test
        void shouldRejectHourOutsideRange() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.TimeWindow(-1, 17, Set.of(DayOfWeek.MONDAY))
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("must be in [0, 23]");
        }

        @Test
        void shouldRejectEmptyDaysOfWeek() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.TimeWindow(9, 17, Set.of())
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("daysOfWeek must not be empty");
        }
    }

    @Nested
    class VelocityValidation {

        @Test
        void shouldRejectZeroMaxTransactions() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.Velocity(0, 60)
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("maxTransactions must be greater than 0");
        }

        @Test
        void shouldRejectZeroPeriodMinutes() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.Velocity(10, 0)
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("periodMinutes must be greater than 0");
        }
    }

    @Nested
    class CooldownValidation {

        @Test
        void shouldRejectZeroCooldownSeconds() {
            // given
            var rules = List.<PolicyRule>of(
                    new PolicyRule.Cooldown(0)
            );

            // when / then
            assertThatThrownBy(() -> policyValidator.validate(rules))
                    .isInstanceOf(InvalidPolicyException.class)
                    .hasMessageContaining("seconds must be greater than 0");
        }
    }
}
