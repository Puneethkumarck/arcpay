package com.arcpay.policy.policyengine.domain.exception;

public class IdentityServiceUnavailableException extends RuntimeException {

    public IdentityServiceUnavailableException(String message) {
        super(message);
    }

    public IdentityServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
