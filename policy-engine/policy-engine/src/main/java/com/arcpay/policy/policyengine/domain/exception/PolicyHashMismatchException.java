package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class PolicyHashMismatchException extends RuntimeException {

    public PolicyHashMismatchException(UUID agentId, String expected, String actual) {
        super("Policy hash mismatch for agent " + agentId + ": expected " + expected + ", got " + actual);
    }
}
