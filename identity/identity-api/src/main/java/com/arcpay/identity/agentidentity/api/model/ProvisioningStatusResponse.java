package com.arcpay.identity.agentidentity.api.model;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ProvisioningStatusResponse(
        UUID agentId, AgentStatusEnum status, List<ProvisioningStepResponse> steps, String failureReason) {}
