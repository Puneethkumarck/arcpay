package com.arcpay.identity.agentidentity.domain.exception;

public class InvalidAgentNameException extends RuntimeException {

    public InvalidAgentNameException(String name) {
        super("Invalid agent name: '" + name + "'");
    }
}
