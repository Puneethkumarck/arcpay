package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentDeactivated(UUID agentId, Instant deactivatedAt) {

    public static final String TOPIC = "agent.deactivated";

    public AgentDeactivated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(deactivatedAt, "deactivatedAt must not be null");
    }
}
