package com.arcpay.identity.agentidentity.domain.exception;

import java.util.UUID;

public class AgentNameDuplicateException extends RuntimeException {

    public AgentNameDuplicateException(String name, UUID ownerId) {
        super("Agent name '" + name + "' already exists for owner " + ownerId);
    }
}
