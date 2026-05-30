package com.arcpay.payment.paymentexecution.domain.exception;

import java.util.UUID;

public class AgentNotOwnedException extends RuntimeException {

    public AgentNotOwnedException(UUID agentId, UUID ownerId) {
        super("Agent " + agentId + " is not owned by " + ownerId);
    }
}
