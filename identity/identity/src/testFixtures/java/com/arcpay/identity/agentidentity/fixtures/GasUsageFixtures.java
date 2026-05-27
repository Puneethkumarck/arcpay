package com.arcpay.identity.agentidentity.fixtures;

import com.arcpay.identity.agentidentity.domain.model.GasUsage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class GasUsageFixtures {

    public static final UUID SOME_OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID SOME_AGENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final Instant SOME_CREATED_AT = Instant.parse("2025-01-15T10:30:00Z");

    public static final GasUsage SOME_GAS_USAGE = GasUsage.builder()
            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .ownerId(SOME_OWNER_ID)
            .agentId(SOME_AGENT_ID)
            .operation("REGISTER_AGENT")
            .txHash("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
            .gasUsed(125_000L)
            .gasCostUsdc(new BigDecimal("0.04250000"))
            .createdAt(SOME_CREATED_AT)
            .build();

    public static final GasUsage SOME_GAS_USAGE_WITHOUT_AGENT = GasUsage.builder()
            .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
            .ownerId(SOME_OWNER_ID)
            .agentId(null)
            .operation("REGISTER_OWNER")
            .txHash("0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
            .gasUsed(80_000L)
            .gasCostUsdc(new BigDecimal("0.02720000"))
            .createdAt(SOME_CREATED_AT)
            .build();

    private GasUsageFixtures() {
    }
}
