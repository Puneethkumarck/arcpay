package com.arcpay.identity.agentidentity.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OwnerResponse(
        UUID ownerId,
        String email,
        String walletAddress,
        String apiKey,
        OwnerStatusEnum status,
        Instant createdAt
) {}
