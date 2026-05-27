package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OwnerRegistered(UUID ownerId, String email, String walletAddress, Instant registeredAt) {

    public static final String TOPIC = "owner.registered";

    public OwnerRegistered {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(walletAddress, "walletAddress must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
    }
}
