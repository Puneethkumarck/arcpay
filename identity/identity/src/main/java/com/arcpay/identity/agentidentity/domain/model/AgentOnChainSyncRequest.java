package com.arcpay.identity.agentidentity.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record AgentOnChainSyncRequest(UUID agentId, OnChainOperation operation, Map<String, String> parameters) {

    public AgentOnChainSyncRequest {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        parameters = Map.copyOf(parameters);
    }
}
