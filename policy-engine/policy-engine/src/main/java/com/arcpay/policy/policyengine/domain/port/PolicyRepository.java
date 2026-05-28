package com.arcpay.policy.policyengine.domain.port;

import com.arcpay.policy.policyengine.domain.model.Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository {

    Policy save(Policy policy);

    Optional<Policy> findById(UUID policyId);

    Optional<Policy> findActiveByAgentId(UUID agentId);

    Page<Policy> findByAgentId(UUID agentId, Pageable pageable);

    Optional<Policy> findByAgentIdAndPolicyId(UUID agentId, UUID policyId);

    Optional<Integer> findMaxVersionByAgentId(UUID agentId);
}
