package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PolicyCreationService {

    public Policy createPolicy(UUID agentId, UUID ownerId, List<PolicyRule> rules,
                               String policyHash, int version) {
        var policyId = UuidCreator.getTimeOrderedEpoch();
        var now = Instant.now();

        return Policy.builder()
                .policyId(policyId)
                .agentId(agentId)
                .ownerId(ownerId)
                .version(version)
                .rules(rules)
                .policyHash(policyHash)
                .status(PolicyStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
