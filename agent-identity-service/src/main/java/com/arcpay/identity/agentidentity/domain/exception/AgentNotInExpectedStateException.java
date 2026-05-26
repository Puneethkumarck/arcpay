package com.arcpay.identity.agentidentity.domain.exception;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;

import java.util.UUID;

public class AgentNotInExpectedStateException extends RuntimeException {

    public AgentNotInExpectedStateException(UUID agentId, AgentStatus currentStatus, AgentStatus expectedStatus) {
        super("Agent " + agentId + " is in status " + currentStatus + " but expected " + expectedStatus);
    }
}
