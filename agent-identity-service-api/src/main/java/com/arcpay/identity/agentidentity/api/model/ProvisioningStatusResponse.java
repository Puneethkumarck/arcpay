package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ProvisioningStatusResponse(
        UUID agentId,
        AgentStatusEnum status,
        List<ProvisioningStepResponse> steps,
        String failureReason
) {}
