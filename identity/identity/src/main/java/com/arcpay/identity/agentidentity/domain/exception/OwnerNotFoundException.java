package com.arcpay.identity.agentidentity.domain.exception;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException(String apiKeyHash) {
        super("Owner not found for api-key-hash: " + apiKeyHash);
    }
}
