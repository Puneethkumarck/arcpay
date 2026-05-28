package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class PolicyViolationException extends RuntimeException {

    public PolicyViolationException(UUID agentId, String violation) {
        super("Policy violation for agent " + agentId + ": " + violation);
    }
}
