package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AgentProvisioningRequest(
        UUID agentId,
        UUID ownerId,
        String name,
        String purpose,
        String metadataHash
) {}
