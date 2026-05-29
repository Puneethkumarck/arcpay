package com.arcpay.policy.policyengine.domain.agent;

import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.model.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentAuthorization {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AgentServiceClient agentServiceClient;

    public AgentInfo verifyOwnership(UUID agentId, UUID ownerId) {
        var agent = agentServiceClient.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new AgentOwnershipException(agentId, ownerId);
        }
        return agent;
    }

    public AgentInfo verifyOwnershipAndActive(UUID agentId, UUID ownerId) {
        var agent = verifyOwnership(agentId, ownerId);
        if (!ACTIVE_STATUS.equals(agent.status())) {
            throw new AgentNotActiveException(agentId, agent.status());
        }
        return agent;
    }
}
