package com.arcpay.identity.agentidentity.api.model;

public final class OwnerRequestFixtures {

    public static final String SOME_EMAIL = "alice@example.com";
    public static final String SOME_WALLET_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678";

    public static final RegisterOwnerRequest SOME_REGISTER_OWNER_REQUEST = new RegisterOwnerRequest(
            SOME_EMAIL,
            SOME_WALLET_ADDRESS
    );

    private OwnerRequestFixtures() {}
}
