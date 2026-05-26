package com.arcpay.identity.agentidentity.domain.model;

import java.util.UUID;

public record ProvisioningStatus(
        UUID agentId,
        AgentStatus overallStatus,
        StepStatus walletCreation,
        StepStatus onChainRegistration
) {}
