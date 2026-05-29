package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.policy.PolicyHashUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PolicyFixtures {

    private PolicyFixtures() {}

    public static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000001");
    public static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    public static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000003");
    public static final String SOME_OWNER_EMAIL = "owner@arcpay.test";
    public static final OwnerPrincipal SOME_OWNER_PRINCIPAL = new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
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

    /** Hash actually produced by {@link PolicyHashUtil#computePolicyHash} for {@link #SOME_RULES}. */
    public static final String SOME_COMPUTED_HASH = PolicyHashUtil.computePolicyHash(SOME_RULES);

    /** ACTIVE policy whose hash matches the computed hash of {@link #SOME_RULES} — used for idempotency tests. */
    public static final Policy SOME_ACTIVE_POLICY_WITH_COMPUTED_HASH = SOME_ACTIVE_POLICY.toBuilder()
            .policyHash(SOME_COMPUTED_HASH)
            .build();

    public static final AgentInfo SOME_ACTIVE_AGENT = new AgentInfo(
            SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", SOME_POLICY_HASH);

    public static final AgentInfo SOME_SUSPENDED_AGENT = new AgentInfo(
            SOME_AGENT_ID, SOME_OWNER_ID, "SUSPENDED", SOME_POLICY_HASH);

    public static final AgentInfo SOME_AGENT_OWNED_BY_OTHER = new AgentInfo(
            SOME_AGENT_ID, UUID.fromString("019576a0-0000-7000-8000-000000000099"), "ACTIVE", SOME_POLICY_HASH);
}
