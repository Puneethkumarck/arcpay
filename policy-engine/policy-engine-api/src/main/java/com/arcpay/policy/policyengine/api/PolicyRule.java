package com.arcpay.policy.policyengine.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PolicyRule.DailyLimit.class, name = "DAILY_LIMIT"),
        @JsonSubTypes.Type(value = PolicyRule.WeeklyLimit.class, name = "WEEKLY_LIMIT"),
        @JsonSubTypes.Type(value = PolicyRule.MonthlyLimit.class, name = "MONTHLY_LIMIT"),
        @JsonSubTypes.Type(value = PolicyRule.PerTransactionLimit.class, name = "PER_TX_LIMIT"),
        @JsonSubTypes.Type(value = PolicyRule.RecipientAllowlist.class, name = "RECIPIENT_ALLOWLIST"),
        @JsonSubTypes.Type(value = PolicyRule.RecipientBlocklist.class, name = "RECIPIENT_BLOCKLIST"),
        @JsonSubTypes.Type(value = PolicyRule.TimeWindow.class, name = "TIME_WINDOW"),
        @JsonSubTypes.Type(value = PolicyRule.Velocity.class, name = "VELOCITY"),
        @JsonSubTypes.Type(value = PolicyRule.ApprovalThreshold.class, name = "APPROVAL_THRESHOLD"),
        @JsonSubTypes.Type(value = PolicyRule.Cooldown.class, name = "COOLDOWN")
})
public sealed interface PolicyRule {

    record DailyLimit(BigDecimal amount) implements PolicyRule {}
    record WeeklyLimit(BigDecimal amount) implements PolicyRule {}
    record MonthlyLimit(BigDecimal amount) implements PolicyRule {}
    record PerTransactionLimit(BigDecimal amount) implements PolicyRule {}
    record RecipientAllowlist(Set<String> addresses) implements PolicyRule {}
    record RecipientBlocklist(Set<String> addresses) implements PolicyRule {}
    record TimeWindow(int startHour, int endHour, Set<DayOfWeek> daysOfWeek) implements PolicyRule {}
    record Velocity(int maxTransactions, int periodMinutes) implements PolicyRule {}
    record ApprovalThreshold(BigDecimal amount) implements PolicyRule {}
    record Cooldown(int seconds) implements PolicyRule {}
}
