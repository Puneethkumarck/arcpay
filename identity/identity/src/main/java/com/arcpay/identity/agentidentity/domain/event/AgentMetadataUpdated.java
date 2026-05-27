package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentMetadataUpdated(UUID agentId, String name, String purpose, String metadataHash,
                                   Instant updatedAt) {

    public static final String TOPIC = "agent.metadata-updated";

    public AgentMetadataUpdated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(metadataHash, "metadataHash must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
