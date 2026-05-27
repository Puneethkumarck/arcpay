package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
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

    public Owner {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(walletAddress, "walletAddress must not be null");
        Objects.requireNonNull(apiKeyHash, "apiKeyHash must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
