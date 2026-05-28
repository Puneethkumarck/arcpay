package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PolicyFixtures {

    private PolicyFixtures() {}

    public static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000001");
    public static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    public static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000003");
    public static final String SOME_POLICY_HASH = "0xabc123def456";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    public static final PolicyRule SOME_DAILY_LIMIT = new PolicyRule.DailyLimit(new BigDecimal("1000.00"));
    public static final PolicyRule SOME_PER_TX_LIMIT = new PolicyRule.PerTransactionLimit(new BigDecimal("100.00"));

    public static final List<PolicyRule> SOME_RULES = List.of(SOME_DAILY_LIMIT, SOME_PER_TX_LIMIT);

    public static final Policy SOME_ACTIVE_POLICY = Policy.builder()
            .policyId(SOME_POLICY_ID)
            .agentId(SOME_AGENT_ID)
            .ownerId(SOME_OWNER_ID)
            .version(1)
            .rules(SOME_RULES)
            .policyHash(SOME_POLICY_HASH)
            .status(PolicyStatus.ACTIVE)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static final Policy SOME_SUPERSEDED_POLICY = SOME_ACTIVE_POLICY.toBuilder()
            .status(PolicyStatus.SUPERSEDED)
            .build();
}
