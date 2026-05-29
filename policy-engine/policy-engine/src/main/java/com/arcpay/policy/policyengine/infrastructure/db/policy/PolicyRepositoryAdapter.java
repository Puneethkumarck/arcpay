package com.arcpay.policy.policyengine.infrastructure.db.policy;

import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.infrastructure.db.policy.mapper.PolicyEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PolicyRepositoryAdapter implements PolicyRepository {

    private final PolicyJpaRepository jpaRepository;
    private final PolicyEntityMapper mapper;

    @Override
    public Policy save(Policy policy) {
        var entity = mapper.mapToEntity(policy);
        var saved = jpaRepository.save(entity);
        return mapper.mapToDomain(saved);
    }

    @Override
    public Optional<Policy> findById(UUID policyId) {
        return jpaRepository.findById(policyId).map(mapper::mapToDomain);
    }

    @Override
    public Optional<Policy> findActiveByAgentId(UUID agentId) {
        return jpaRepository.findByAgentIdAndStatus(agentId, PolicyStatus.ACTIVE)
                .map(mapper::mapToDomain);
    }

    @Override
    public Page<Policy> findByAgentId(UUID agentId, Pageable pageable) {
        return jpaRepository.findByAgentIdOrderByVersionDesc(agentId, pageable)
                .map(mapper::mapToDomain);
    }

    @Override
    public Optional<Policy> findByAgentIdAndPolicyId(UUID agentId, UUID policyId) {
        return jpaRepository.findByAgentIdAndPolicyId(agentId, policyId)
                .map(mapper::mapToDomain);
    }

    @Override
    public Optional<Integer> findMaxVersionByAgentId(UUID agentId) {
        return jpaRepository.findMaxVersionByAgentId(agentId);
    }
}
