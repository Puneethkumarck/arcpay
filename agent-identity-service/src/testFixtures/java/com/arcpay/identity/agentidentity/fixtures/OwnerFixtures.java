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
    public static final String SOME_CHECKSUMMED_WALLET = "0x1234567890AbCdEf1234567890aBcDeF12345678";
    public static final String SOME_API_KEY_HASH = "a".repeat(64);
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-06-01T10:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-06-01T10:00:00Z");

    public static final UUID OTHER_OWNER_ID = UUID.fromString("019718a0-9999-7def-8000-abcdef999999");
    public static final String OTHER_EMAIL = "bob@example.com";
    public static final String OTHER_WALLET_ADDRESS = "0xfedcba9876543210fedcba9876543210fedcba98";
    public static final String OTHER_API_KEY_HASH = "b".repeat(64);

    public static final Owner SOME_OWNER = Owner.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_EMAIL)
            .walletAddress(SOME_WALLET_ADDRESS)
            .apiKeyHash(SOME_API_KEY_HASH)
            .status(OwnerStatus.ACTIVE)
            .createdAt(SOME_CREATED_AT)
            .updatedAt(SOME_UPDATED_AT)
            .build();

    public static OwnerEntity someOwnerEntity() {
        return OwnerEntity.builder()
                .ownerId(SOME_OWNER_ID)
                .email(SOME_EMAIL)
                .walletAddress(SOME_WALLET_ADDRESS)
                .apiKeyHash(SOME_API_KEY_HASH)
                .status(OwnerStatus.ACTIVE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();
    }

    public static OwnerEntity otherOwnerEntity() {
        return OwnerEntity.builder()
                .ownerId(OTHER_OWNER_ID)
                .email(OTHER_EMAIL)
                .walletAddress(OTHER_WALLET_ADDRESS)
                .apiKeyHash(OTHER_API_KEY_HASH)
                .status(OwnerStatus.ACTIVE)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();
    }

    private OwnerFixtures() {
    }
}
