package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentReactivated(UUID agentId, Instant reactivatedAt) {

    public static final String TOPIC = "agent.reactivated";

    public AgentReactivated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(reactivatedAt, "reactivatedAt must not be null");
    }
}
