package com.arcpay.identity.agentidentity.domain.exception;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException() {
        super("Owner not found");
    }
}
