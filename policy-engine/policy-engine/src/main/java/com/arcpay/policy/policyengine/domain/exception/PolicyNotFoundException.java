package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(UUID policyId) {
        super("Policy not found: " + policyId);
    }

    public PolicyNotFoundException(UUID agentId, String context) {
        super("No active policy found for agent " + agentId + ": " + context);
    }
}
