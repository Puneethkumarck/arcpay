package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Owner(
        UUID ownerId,
        String email,
        String walletAddress,
        String apiKeyHash,
        OwnerStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
