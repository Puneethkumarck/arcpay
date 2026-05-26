package com.arcpay.identity.agentidentity.domain.exception;

public class InvalidPolicyHashException extends RuntimeException {

    public InvalidPolicyHashException(String policyHash) {
        super("Invalid policy hash: '" + policyHash + "'");
    }
}
