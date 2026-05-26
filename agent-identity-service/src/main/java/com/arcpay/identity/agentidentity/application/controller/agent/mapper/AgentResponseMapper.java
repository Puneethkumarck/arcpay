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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AgentResponseMapper {

    @Mapping(source = "status", target = "status")
    AgentResponse toApi(Agent agent);

    default AgentStatusEnum map(AgentStatus status) {
        return AgentStatusEnum.valueOf(status.name());
    }

    default AgentListResponse toApi(Page<Agent> page) {
        var content = page.getContent().stream().map(this::toApi).toList();
        return AgentListResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    default ProvisioningStatusResponse toApi(AgentQueryHandler.ProvisioningStatus status) {
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
                .status(map(status.overallStatus()))
                .steps(steps)
                .build();
    }

    default StepStatusEnum mapStepStatus(AgentQueryHandler.StepStatus stepStatus) {
        return StepStatusEnum.valueOf(stepStatus.name());
    }
}
