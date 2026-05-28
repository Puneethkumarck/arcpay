package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class AgentNotFoundException extends RuntimeException {

    public AgentNotFoundException(UUID agentId) {
        super("Agent not found: " + agentId);
    }
}
