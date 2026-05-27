package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentRegistrationRequested(UUID agentId, UUID ownerId, String name, String purpose,
                                         String metadataHash, Instant requestedAt) {

    public static final String TOPIC = "agent.registration-requested";

    public AgentRegistrationRequested {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(metadataHash, "metadataHash must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
