package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record IdempotencyKey(
        UUID idempotencyKey,
        UUID ownerId,
        String endpoint,
        int responseStatus,
        String responseBody,
        Instant createdAt,
        Instant expiresAt
) {
}
