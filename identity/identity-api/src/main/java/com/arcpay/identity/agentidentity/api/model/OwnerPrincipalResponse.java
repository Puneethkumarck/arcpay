package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record OwnerPrincipalResponse(
        UUID ownerId,
        String email,
        String authority
) {

    private static final String DEFAULT_AUTHORITY = "OWNER";

    public OwnerPrincipalResponse {
        if (authority == null || authority.isBlank()) {
            authority = DEFAULT_AUTHORITY;
        }
    }
}
