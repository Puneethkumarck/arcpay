package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AgentResponse(
        UUID agentId,
        UUID ownerId,
        String name,
        String purpose,
        AgentStatusEnum status,
        String walletId,
        String walletAddress,
        String onChainTxHash,
        String policyHash,
        String metadataHash,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {}
