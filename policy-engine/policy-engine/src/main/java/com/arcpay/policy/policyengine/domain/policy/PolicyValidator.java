package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PolicyValidator {

    public void validate(List<PolicyRule> rules) {
        if (rules == null) {
            throw new InvalidPolicyException("Rules list must not be null");
        }
        validateNotEmpty(rules);
        validateNoDuplicateTypes(rules);
        validateLimitHierarchy(rules);
        validateNoAddressOverlap(rules);
        rules.forEach(this::validateRule);
    }

    private void validateNotEmpty(List<PolicyRule> rules) {
        if (rules.isEmpty()) {
            throw new InvalidPolicyException("Rules list must not be empty");
        }
    }

    private void validateNoDuplicateTypes(List<PolicyRule> rules) {
        var seen = new HashSet<Class<?>>();
        for (var rule : rules) {
            if (!seen.add(rule.getClass())) {
                throw new InvalidPolicyException("Duplicate rule type: " + rule.getClass().getSimpleName());
            }
        }
    }

    private void validateLimitHierarchy(List<PolicyRule> rules) {
        var daily = findAmount(rules, PolicyRule.DailyLimit.class);
        var weekly = findAmount(rules, PolicyRule.WeeklyLimit.class);
        var monthly = findAmount(rules, PolicyRule.MonthlyLimit.class);

        requireOrdered(daily, weekly, "DAILY_LIMIT amount must be <= WEEKLY_LIMIT amount");
        requireOrdered(weekly, monthly, "WEEKLY_LIMIT amount must be <= MONTHLY_LIMIT amount");
        requireOrdered(daily, monthly, "DAILY_LIMIT amount must be <= MONTHLY_LIMIT amount");
    }

    private void requireOrdered(Optional<BigDecimal> lower, Optional<BigDecimal> upper, String message) {
        lower.flatMap(low -> upper.filter(high -> low.compareTo(high) > 0))
                .ifPresent(violation -> {
                    throw new InvalidPolicyException(message);
                });
    }

    private Optional<BigDecimal> findAmount(List<PolicyRule> rules, Class<? extends PolicyRule> type) {
        return rules.stream()
                .filter(type::isInstance)
                .map(r -> switch (r) {
                    case PolicyRule.DailyLimit dl -> dl.amount();
                    case PolicyRule.WeeklyLimit wl -> wl.amount();
                    case PolicyRule.MonthlyLimit ml -> ml.amount();
                    default -> throw new IllegalStateException("Unexpected type: " + r.getClass());
                })
                .findFirst();
    }

    private void validateNoAddressOverlap(List<PolicyRule> rules) {
        var allowlist = rules.stream()
                .filter(PolicyRule.RecipientAllowlist.class::isInstance)
                .map(PolicyRule.RecipientAllowlist.class::cast)
                .flatMap(r -> r.addresses().stream())
                .map(address -> address.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        var blocklist = rules.stream()
                .filter(PolicyRule.RecipientBlocklist.class::isInstance)
                .map(PolicyRule.RecipientBlocklist.class::cast)
                .flatMap(r -> r.addresses().stream())
                .map(address -> address.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        var overlap = new HashSet<>(allowlist);
        overlap.retainAll(blocklist);

        if (!overlap.isEmpty()) {
            throw new InvalidPolicyException(
                    "Address overlap between allowlist and blocklist: " + overlap);
        }
    }

    private void validateRule(PolicyRule rule) {
        switch (rule) {
            case PolicyRule.DailyLimit dl -> validatePositiveAmount(dl.amount(), "DAILY_LIMIT");
            case PolicyRule.WeeklyLimit wl -> validatePositiveAmount(wl.amount(), "WEEKLY_LIMIT");
            case PolicyRule.MonthlyLimit ml -> validatePositiveAmount(ml.amount(), "MONTHLY_LIMIT");
            case PolicyRule.PerTransactionLimit ptl -> validatePositiveAmount(ptl.amount(), "PER_TX_LIMIT");
            case PolicyRule.ApprovalThreshold at -> validatePositiveAmount(at.amount(), "APPROVAL_THRESHOLD");
            case PolicyRule.RecipientAllowlist ra -> validateAddressSet(ra.addresses(), "RECIPIENT_ALLOWLIST");
            case PolicyRule.RecipientBlocklist rb -> validateAddressSet(rb.addresses(), "RECIPIENT_BLOCKLIST");
            case PolicyRule.TimeWindow tw -> validateTimeWindow(tw);
            case PolicyRule.Velocity v -> validateVelocity(v);
            case PolicyRule.Cooldown c -> validateCooldown(c);
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String ruleName) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPolicyException(ruleName + " amount must be greater than 0");
        }
    }

    private void validateAddressSet(Set<String> addresses, String ruleName) {
        if (addresses.isEmpty()) {
            throw new InvalidPolicyException(ruleName + " addresses must not be empty");
        }
        if (addresses.size() > 100) {
            throw new InvalidPolicyException(ruleName + " addresses must not exceed 100 entries");
        }
    }

    private void validateTimeWindow(PolicyRule.TimeWindow tw) {
        if (tw.startHour() < 0 || tw.startHour() > 23) {
            throw new InvalidPolicyException("TIME_WINDOW startHour must be in [0, 23]");
        }
        if (tw.endHour() < 0 || tw.endHour() > 23) {
            throw new InvalidPolicyException("TIME_WINDOW endHour must be in [0, 23]");
        }
        if (tw.startHour() >= tw.endHour()) {
            throw new InvalidPolicyException("TIME_WINDOW startHour must be less than endHour");
        }
        if (tw.daysOfWeek().isEmpty()) {
            throw new InvalidPolicyException("TIME_WINDOW daysOfWeek must not be empty");
        }
    }

    private void validateVelocity(PolicyRule.Velocity v) {
        if (v.maxTransactions() <= 0) {
            throw new InvalidPolicyException("VELOCITY maxTransactions must be greater than 0");
        }
        if (v.periodMinutes() <= 0) {
            throw new InvalidPolicyException("VELOCITY periodMinutes must be greater than 0");
        }
    }

    private void validateCooldown(PolicyRule.Cooldown c) {
        if (c.seconds() <= 0) {
            throw new InvalidPolicyException("COOLDOWN seconds must be greater than 0");
        }
    }
}
