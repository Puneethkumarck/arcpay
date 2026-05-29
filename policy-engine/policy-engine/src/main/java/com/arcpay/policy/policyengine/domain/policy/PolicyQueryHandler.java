package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PolicyQueryHandler {

    private final AgentServiceClient agentServiceClient;
    private final PolicyRepository policyRepository;

    public Policy getActivePolicy(UUID agentId, UUID ownerId) {
        verifyOwnership(agentId, ownerId);
        return policyRepository.findActiveByAgentId(agentId)
                .orElseThrow(() -> new PolicyNotFoundException(agentId, "no active policy"));
    }

    public Policy getPolicy(UUID agentId, UUID policyId, UUID ownerId) {
        verifyOwnership(agentId, ownerId);
        return policyRepository.findByAgentIdAndPolicyId(agentId, policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }

    public Page<Policy> listPolicyHistory(UUID agentId, UUID ownerId, Pageable pageable) {
        verifyOwnership(agentId, ownerId);
        return policyRepository.findByAgentId(agentId, pageable);
    }

    private AgentInfo verifyOwnership(UUID agentId, UUID ownerId) {
        var agent = agentServiceClient.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new AgentOwnershipException(agentId, ownerId);
        }
        return agent;
    }
}
