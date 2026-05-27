package com.arcpay.identity.agentidentity.domain.exception;

public class OwnerEmailAlreadyExistsException extends RuntimeException {

    public OwnerEmailAlreadyExistsException(String email) {
        super("Owner with email '" + email + "' already exists");
    }
}
