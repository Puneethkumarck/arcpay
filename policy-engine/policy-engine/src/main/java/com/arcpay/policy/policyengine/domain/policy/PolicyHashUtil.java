package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.erdtman.jcs.JsonCanonicalizer;
import org.web3j.crypto.Hash;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PolicyHashUtil {

    private PolicyHashUtil() {}

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Discriminator names derived from the {@link JsonSubTypes} metadata declared on
     * {@link PolicyRule} — the single source of truth for type names. Deriving the map this
     * way guarantees that any future sealed subtype that registers a {@code @JsonSubTypes.Type}
     * is automatically picked up, and a subtype missing from that registration fails loudly
     * via {@link #typeName(PolicyRule)} instead of producing a {@code null} sort key.
     */
    private static final Map<Class<?>, String> TYPE_NAMES = Arrays.stream(
                    PolicyRule.class.getAnnotation(JsonSubTypes.class).value())
            .collect(Collectors.toUnmodifiableMap(JsonSubTypes.Type::value, JsonSubTypes.Type::name));

    public static String computePolicyHash(List<PolicyRule> rules) {
        try {
            var normalized = rules.stream()
                    .sorted(Comparator.comparing(PolicyHashUtil::typeName))
                    .map(PolicyHashUtil::normalize)
                    .toList();
            var json = objectMapper.writeValueAsString(normalized);
            var canonical = new JsonCanonicalizer(json).getEncodedString();
            return Hash.sha3String(canonical);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize policy rules to JSON", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to canonicalize policy rules JSON", e);
        }
    }

    private static String typeName(PolicyRule rule) {
        var name = TYPE_NAMES.get(rule.getClass());
        if (name == null) {
            throw new IllegalStateException(
                    "PolicyRule subtype is not registered in @JsonSubTypes on PolicyRule: "
                            + rule.getClass().getName());
        }
        return name;
    }

    /**
     * Builds a normalized, JSON-serializable representation of a rule used only for hashing.
     * JCS (RFC 8785) canonicalizes object keys but preserves array element order, and
     * {@link Set} iteration order is nondeterministic. To make the hash deterministic, every
     * {@code Set} field is converted to a sorted {@link List} here. The input domain objects
     * are never mutated — a fresh map carrying the discriminator and sorted collections is
     * produced for each rule.
     */
    private static Map<String, Object> normalize(PolicyRule rule) {
        var type = typeName(rule);
        return switch (rule) {
            case PolicyRule.DailyLimit r -> Map.of("type", type, "amount", amount(r.amount()));
            case PolicyRule.WeeklyLimit r -> Map.of("type", type, "amount", amount(r.amount()));
            case PolicyRule.MonthlyLimit r -> Map.of("type", type, "amount", amount(r.amount()));
            case PolicyRule.PerTransactionLimit r -> Map.of("type", type, "amount", amount(r.amount()));
            case PolicyRule.ApprovalThreshold r -> Map.of("type", type, "amount", amount(r.amount()));
            case PolicyRule.RecipientAllowlist r -> Map.of("type", type, "addresses", sortAddresses(r.addresses()));
            case PolicyRule.RecipientBlocklist r -> Map.of("type", type, "addresses", sortAddresses(r.addresses()));
            case PolicyRule.TimeWindow r -> Map.of(
                    "type", type,
                    "startHour", r.startHour(),
                    "endHour", r.endHour(),
                    "daysOfWeek", sortDays(r.daysOfWeek()));
            case PolicyRule.Velocity r -> Map.of(
                    "type", type,
                    "maxTransactions", r.maxTransactions(),
                    "periodMinutes", r.periodMinutes());
            case PolicyRule.Cooldown r -> Map.of("type", type, "seconds", r.seconds());
        };
    }

    /**
     * Renders a monetary amount as its exact decimal text for hashing. JCS (RFC 8785) normalizes
     * JSON <em>numbers</em> through IEEE-754 double / ECMAScript formatting, which drops trailing
     * zeros ({@code 1000.00} → {@code 1000}) and cannot represent every high-scale/large
     * {@code BigDecimal} exactly — risking hash drift and collisions. Emitting the amount as a JSON
     * string preserves the exact value; {@code stripTrailingZeros} keeps logically-equal amounts
     * (e.g. {@code 1000.00} and {@code 1000}) hashing identically for idempotency.
     */
    private static String amount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static List<String> sortAddresses(Set<String> addresses) {
        return addresses.stream()
                .map(a -> a.toLowerCase(Locale.ROOT))
                .sorted()
                .toList();
    }

    private static List<String> sortDays(Set<DayOfWeek> days) {
        return days.stream()
                .sorted()
                .map(DayOfWeek::name)
                .toList();
    }
}
