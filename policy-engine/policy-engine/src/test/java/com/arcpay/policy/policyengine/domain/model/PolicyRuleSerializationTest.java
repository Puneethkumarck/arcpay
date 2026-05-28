package com.arcpay.policy.policyengine.domain.model;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRuleSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    static Stream<Arguments> allRuleTypes() {
        return Stream.of(
                Arguments.of(new PolicyRule.DailyLimit(new BigDecimal("1000.00")), "DAILY_LIMIT"),
                Arguments.of(new PolicyRule.WeeklyLimit(new BigDecimal("5000.00")), "WEEKLY_LIMIT"),
                Arguments.of(new PolicyRule.MonthlyLimit(new BigDecimal("20000.00")), "MONTHLY_LIMIT"),
                Arguments.of(new PolicyRule.PerTransactionLimit(new BigDecimal("500.00")), "PER_TX_LIMIT"),
                Arguments.of(new PolicyRule.RecipientAllowlist(Set.of("0x1234567890abcdef1234567890abcdef12345678")), "RECIPIENT_ALLOWLIST"),
                Arguments.of(new PolicyRule.RecipientBlocklist(Set.of("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef")), "RECIPIENT_BLOCKLIST"),
                Arguments.of(new PolicyRule.TimeWindow(9, 17, Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)), "TIME_WINDOW"),
                Arguments.of(new PolicyRule.Velocity(10, 60), "VELOCITY"),
                Arguments.of(new PolicyRule.ApprovalThreshold(new BigDecimal("10000.00")), "APPROVAL_THRESHOLD"),
                Arguments.of(new PolicyRule.Cooldown(300), "COOLDOWN")
        );
    }

    @Nested
    class RoundTrip {

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("com.arcpay.policy.policyengine.domain.model.PolicyRuleSerializationTest#allRuleTypes")
        void shouldSerializeAndDeserializeAllRuleTypes(PolicyRule rule, String expectedType) {
            // when
            var json = objectMapper.writeValueAsString(rule);
            var deserialized = objectMapper.readValue(json, PolicyRule.class);

            // then
            assertThat(deserialized)
                    .usingRecursiveComparison()
                    .isEqualTo(rule);
        }
    }

    @Nested
    class TypeDiscriminator {

        @SneakyThrows
        @ParameterizedTest
        @MethodSource("com.arcpay.policy.policyengine.domain.model.PolicyRuleSerializationTest#allRuleTypes")
        void shouldIncludeTypeDiscriminator(PolicyRule rule, String expectedType) {
            // when
            var json = objectMapper.writeValueAsString(rule);

            // then
            assertThat(json).contains("\"type\":\"" + expectedType + "\"");
        }
    }

    @Nested
    class SpecificTypes {

        @SneakyThrows
        @Test
        void shouldSerializeAndDeserializeDailyLimit() {
            // given
            var rule = new PolicyRule.DailyLimit(new BigDecimal("1000.00"));

            // when
            var json = objectMapper.writeValueAsString(rule);
            var deserialized = objectMapper.readValue(json, PolicyRule.class);

            // then
            assertThat(deserialized).isInstanceOf(PolicyRule.DailyLimit.class);
            assertThat(deserialized)
                    .usingRecursiveComparison()
                    .isEqualTo(rule);
        }
    }
}
