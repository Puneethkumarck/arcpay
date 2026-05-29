package com.arcpay.platform.api;

import java.util.Objects;
import java.util.UUID;

public record OwnerPrincipal(UUID ownerId, String email, String authority) {

    private static final String DEFAULT_AUTHORITY = "OWNER";

    public OwnerPrincipal {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        if (authority == null || authority.isBlank()) {
            authority = DEFAULT_AUTHORITY;
        }
    }

    public OwnerPrincipal(UUID ownerId, String email) {
        this(ownerId, email, DEFAULT_AUTHORITY);
    }
}
