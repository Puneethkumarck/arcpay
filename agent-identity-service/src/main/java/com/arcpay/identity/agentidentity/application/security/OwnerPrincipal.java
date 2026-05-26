package com.arcpay.identity.agentidentity.application.security;

import java.util.Objects;
import java.util.UUID;

public record OwnerPrincipal(UUID ownerId, String email) {

    public OwnerPrincipal {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(email, "email must not be null");
    }
}
