package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class AgentNotActiveException extends RuntimeException {

    public AgentNotActiveException(UUID agentId, String status) {
        super("Agent " + agentId + " is not active, current status: " + status);
    }
}
