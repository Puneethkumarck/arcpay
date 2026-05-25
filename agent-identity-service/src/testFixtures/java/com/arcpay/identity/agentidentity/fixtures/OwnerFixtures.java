package com.arcpay.identity.agentidentity.fixtures;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
import com.arcpay.identity.agentidentity.infrastructure.db.owner.OwnerEntity;

import java.time.Instant;
import java.util.UUID;

public final class OwnerFixtures {

    public static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    public static final String SOME_EMAIL = "alice@example.com";
    public static final String SOME_WALLET_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678";
    public static final String SOME_API_KEY_HASH = "a".repeat(64);
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-06-01T10:00:00Z");

    public static final Owner SOME_OWNER = Owner.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_EMAIL)
            .walletAddress(SOME_WALLET_ADDRESS)
            .apiKeyHash(SOME_API_KEY_HASH)
            .status(OwnerStatus.ACTIVE)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static final OwnerEntity SOME_OWNER_ENTITY = OwnerEntity.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_EMAIL)
            .walletAddress(SOME_WALLET_ADDRESS)
            .apiKeyHash(SOME_API_KEY_HASH)
            .status(OwnerStatus.ACTIVE)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    private OwnerFixtures() {
    }
}
