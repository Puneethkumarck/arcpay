package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record ProvisioningStatus(
        UUID agentId,
        AgentStatus overallStatus,
        StepStatus walletCreation,
        StepStatus onChainRegistration
) {}
