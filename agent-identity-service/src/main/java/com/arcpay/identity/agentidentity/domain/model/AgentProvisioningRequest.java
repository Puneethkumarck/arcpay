package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record AgentProvisioningRequest(
        UUID agentId,
        UUID ownerId,
        String name,
        String purpose,
        String metadataHash
) {

    public AgentProvisioningRequest {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(metadataHash, "metadataHash must not be null");
    }
}
