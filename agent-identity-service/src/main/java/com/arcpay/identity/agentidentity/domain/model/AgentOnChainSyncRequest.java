package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
public record AgentOnChainSyncRequest(
        UUID agentId,
        OnChainOperation operation,
        Map<String, String> parameters
) {}
