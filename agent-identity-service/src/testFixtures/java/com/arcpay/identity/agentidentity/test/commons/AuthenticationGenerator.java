package com.arcpay.identity.agentidentity.test.commons;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;

import java.time.Instant;
import java.util.UUID;

public final class AuthenticationGenerator {

    private static final Instant FIXED_TIMESTAMP = Instant.parse("2026-06-01T10:00:00Z");
    private static final String REALISTIC_API_KEY_HASH = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private AuthenticationGenerator() {}

    public static Owner authenticatedOwner() {
        return Owner.builder()
                .ownerId(UUID.fromString("019718a0-1234-7def-8000-abcdef123456"))
                .email("test-owner@example.com")
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash(REALISTIC_API_KEY_HASH)
                .status(OwnerStatus.ACTIVE)
                .createdAt(FIXED_TIMESTAMP)
                .updatedAt(FIXED_TIMESTAMP)
                .build();
    }

    public static Owner authenticatedOwner(UUID ownerId) {
        return Owner.builder()
                .ownerId(ownerId)
                .email("test-owner@example.com")
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash(REALISTIC_API_KEY_HASH)
                .status(OwnerStatus.ACTIVE)
                .createdAt(FIXED_TIMESTAMP)
                .updatedAt(FIXED_TIMESTAMP)
                .build();
    }
}
