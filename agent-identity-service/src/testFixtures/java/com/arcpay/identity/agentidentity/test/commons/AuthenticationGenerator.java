package com.arcpay.identity.agentidentity.test.commons;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;

import java.time.Instant;
import java.util.UUID;

public final class AuthenticationGenerator {

    private AuthenticationGenerator() {}

    public static Owner authenticatedOwner() {
        return Owner.builder()
                .ownerId(UUID.fromString("019718a0-1234-7def-8000-abcdef123456"))
                .email("test-owner@example.com")
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash("a".repeat(64))
                .status(OwnerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Owner authenticatedOwner(UUID ownerId) {
        return Owner.builder()
                .ownerId(ownerId)
                .email("test-owner@example.com")
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash("a".repeat(64))
                .status(OwnerStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
