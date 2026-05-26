package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentActivated(UUID agentId, Instant activatedAt) {

    public static final String TOPIC = "agent.activated";

    public AgentActivated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(activatedAt, "activatedAt must not be null");
    }
}
