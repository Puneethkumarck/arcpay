package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record OwnerPrincipalResponse(
        UUID ownerId,
        String email
) {}
