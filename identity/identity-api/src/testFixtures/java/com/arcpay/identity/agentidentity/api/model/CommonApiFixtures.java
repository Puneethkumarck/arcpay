package com.arcpay.identity.agentidentity.api.model;

import java.time.Instant;
import java.util.UUID;

public final class CommonApiFixtures {

    public static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    public static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:00:00Z");
    public static final String SOME_API_KEY = "ak_test_abcdefghij1234567890abcdefghij12";

    private CommonApiFixtures() {}
}
