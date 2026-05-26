package com.arcpay.identity.agentidentity.domain.exception;

public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String email) {
        super("Invalid email format");
    }
}
