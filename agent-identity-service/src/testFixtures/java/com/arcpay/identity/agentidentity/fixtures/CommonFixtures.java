package com.arcpay.identity.agentidentity.fixtures;

import java.util.UUID;

public final class CommonFixtures {

    public static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    public static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    public static final String SOME_WALLET_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678";
    public static final String SOME_API_KEY = "ak_test_abcdefghij1234567890abcdefghij12";
    public static final String SOME_API_KEY_HASH = "a".repeat(64);

    private CommonFixtures() {}
}
