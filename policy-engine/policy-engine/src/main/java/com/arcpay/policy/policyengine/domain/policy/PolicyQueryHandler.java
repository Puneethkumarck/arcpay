package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.domain.agent.AgentAuthorization;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PolicyQueryHandler {

    private final AgentAuthorization agentAuthorization;
    private final PolicyRepository policyRepository;

    public Policy getActivePolicy(UUID agentId, UUID ownerId) {
        agentAuthorization.verifyOwnership(agentId, ownerId);
        return policyRepository.findActiveByAgentId(agentId)
                .orElseThrow(() -> new PolicyNotFoundException(agentId, "no active policy"));
    }

    public Policy getPolicy(UUID agentId, UUID policyId, UUID ownerId) {
        agentAuthorization.verifyOwnership(agentId, ownerId);
        return policyRepository.findByAgentIdAndPolicyId(agentId, policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
    }

    public Page<Policy> listPolicyHistory(UUID agentId, UUID ownerId, Pageable pageable) {
        agentAuthorization.verifyOwnership(agentId, ownerId);
        return policyRepository.findByAgentId(agentId, pageable);
    }
}
