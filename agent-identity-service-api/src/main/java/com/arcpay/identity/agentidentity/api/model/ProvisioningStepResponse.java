package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ProvisioningStepResponse(
        String name,
        StepStatusEnum status,
        Instant completedAt
) {}
