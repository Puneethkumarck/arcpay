package com.arcpay.identity.agentidentity.domain.model;

import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record ProvisioningStatus(
        UUID agentId, AgentStatus overallStatus, StepStatus walletCreation, StepStatus onChainRegistration) {}
