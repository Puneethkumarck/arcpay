package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.model.ProvisioningStatus;
import com.arcpay.identity.agentidentity.domain.model.StepStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentQueryHandler {

    private final AgentRepository agentRepository;

    public Agent getAgent(UUID agentId, UUID ownerId) {
        var agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new ForbiddenException("agent", ownerId);
        }
        return agent;
    }

    public Page<Agent> listAgents(UUID ownerId, AgentStatus status, Pageable pageable) {
        if (status != null) {
            return agentRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        }
        return agentRepository.findByOwnerId(ownerId, pageable);
    }

    public ProvisioningStatus getProvisioningStatus(UUID agentId, UUID ownerId) {
        var agent = getAgent(agentId, ownerId);
        return deriveProvisioningStatus(agent);
    }

    private ProvisioningStatus deriveProvisioningStatus(Agent agent) {
        return switch (agent.status()) {
            case PROVISIONING -> new ProvisioningStatus(
                    agent.agentId(),
                    agent.status(),
                    StepStatus.IN_PROGRESS,
                    StepStatus.PENDING);
            case WALLET_READY -> new ProvisioningStatus(
                    agent.agentId(),
                    agent.status(),
                    StepStatus.COMPLETED,
                    StepStatus.IN_PROGRESS);
            case ACTIVE -> new ProvisioningStatus(
                    agent.agentId(),
                    agent.status(),
                    StepStatus.COMPLETED,
                    StepStatus.COMPLETED);
            case SUSPENDED -> new ProvisioningStatus(
                    agent.agentId(),
                    agent.status(),
                    StepStatus.COMPLETED,
                    StepStatus.COMPLETED);
            case FAILED -> deriveFailedStatus(agent);
        };
    }

    private ProvisioningStatus deriveFailedStatus(Agent agent) {
        if (agent.walletId() == null) {
            return new ProvisioningStatus(
                    agent.agentId(),
                    agent.status(),
                    StepStatus.FAILED,
                    StepStatus.PENDING);
        }
        return new ProvisioningStatus(
                agent.agentId(),
                agent.status(),
                StepStatus.COMPLETED,
                StepStatus.FAILED);
    }
}
