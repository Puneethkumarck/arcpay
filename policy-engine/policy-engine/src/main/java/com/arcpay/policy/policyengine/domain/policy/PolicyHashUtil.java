package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.erdtman.jcs.JsonCanonicalizer;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PolicyHashUtil {

    private PolicyHashUtil() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<Class<? extends PolicyRule>, String> TYPE_NAMES = Map.ofEntries(
            Map.entry(PolicyRule.ApprovalThreshold.class, "APPROVAL_THRESHOLD"),
            Map.entry(PolicyRule.Cooldown.class, "COOLDOWN"),
            Map.entry(PolicyRule.DailyLimit.class, "DAILY_LIMIT"),
            Map.entry(PolicyRule.MonthlyLimit.class, "MONTHLY_LIMIT"),
            Map.entry(PolicyRule.PerTransactionLimit.class, "PER_TX_LIMIT"),
            Map.entry(PolicyRule.RecipientAllowlist.class, "RECIPIENT_ALLOWLIST"),
            Map.entry(PolicyRule.RecipientBlocklist.class, "RECIPIENT_BLOCKLIST"),
            Map.entry(PolicyRule.TimeWindow.class, "TIME_WINDOW"),
            Map.entry(PolicyRule.Velocity.class, "VELOCITY"),
            Map.entry(PolicyRule.WeeklyLimit.class, "WEEKLY_LIMIT")
    );

    public static String computePolicyHash(List<PolicyRule> rules) {
        try {
            var sorted = rules.stream()
                    .sorted(Comparator.comparing(r -> TYPE_NAMES.get(r.getClass())))
                    .toList();
            var json = objectMapper.writeValueAsString(sorted);
            var canonical = new JsonCanonicalizer(json).getEncodedString();
            return Hash.sha3String(canonical);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize policy rules to JSON", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to canonicalize policy rules JSON", e);
        }
    }
}
