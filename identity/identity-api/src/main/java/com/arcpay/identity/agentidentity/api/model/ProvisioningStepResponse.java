package com.arcpay.identity.agentidentity.api.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ProvisioningStepResponse(String name, StepStatusEnum status, Instant completedAt) {}
