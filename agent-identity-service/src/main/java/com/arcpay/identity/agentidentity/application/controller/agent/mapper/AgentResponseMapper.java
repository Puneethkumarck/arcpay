package com.arcpay.identity.agentidentity.application.controller.agent.mapper;

import com.arcpay.identity.agentidentity.api.model.AgentListResponse;
import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.ProvisioningStatusResponse;
import com.arcpay.identity.agentidentity.api.model.ProvisioningStepResponse;
import com.arcpay.identity.agentidentity.api.model.StepStatusEnum;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentResponseMapper {

    public AgentResponse toApi(Agent agent) {
        return AgentResponse.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(mapStatus(agent.status()))
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .failureReason(agent.failureReason())
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();
    }

    public AgentListResponse toApi(Page<Agent> page) {
        var content = page.getContent().stream().map(this::toApi).toList();
        return AgentListResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public ProvisioningStatusResponse toApi(AgentQueryHandler.ProvisioningStatus status) {
        var steps = List.of(
                ProvisioningStepResponse.builder()
                        .name("WALLET_CREATION")
                        .status(mapStepStatus(status.walletCreation()))
                        .build(),
                ProvisioningStepResponse.builder()
                        .name("ON_CHAIN_REGISTRATION")
                        .status(mapStepStatus(status.onChainRegistration()))
                        .build());
        return ProvisioningStatusResponse.builder()
                .agentId(status.agentId())
                .status(mapStatus(status.overallStatus()))
                .steps(steps)
                .build();
    }

    private AgentStatusEnum mapStatus(AgentStatus status) {
        return AgentStatusEnum.valueOf(status.name());
    }

    private StepStatusEnum mapStepStatus(AgentQueryHandler.StepStatus stepStatus) {
        return StepStatusEnum.valueOf(stepStatus.name());
    }
}
