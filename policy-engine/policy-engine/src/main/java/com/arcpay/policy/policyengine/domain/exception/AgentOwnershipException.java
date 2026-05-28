package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class AgentOwnershipException extends RuntimeException {

    public AgentOwnershipException(UUID agentId, UUID ownerId) {
        super("Agent " + agentId + " does not belong to owner " + ownerId);
    }
}
