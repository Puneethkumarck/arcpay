package com.arcpay.identity.agentidentity.api.model.validator;

public final class ErrorMessages {

    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String WALLET_ADDRESS_REQUIRED = "Wallet address is required";
    public static final String WALLET_ADDRESS_INVALID = "Invalid wallet address: must start with 0x followed by 40 hex characters";
    public static final String AGENT_NAME_REQUIRED = "Agent name is required";
    public static final String AGENT_NAME_SIZE = "Agent name must be between 3 and 64 characters";
    public static final String PURPOSE_REQUIRED = "Purpose is required";
    public static final String PURPOSE_SIZE = "Purpose must not exceed 256 characters";
    public static final String POLICY_HASH_REQUIRED = "Policy hash is required";
    public static final String POLICY_HASH_INVALID = "Invalid policy hash: must start with 0x followed by 64 hex characters";

    private ErrorMessages() {}
}
