package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class SpendingLockRepositoryAdapter implements SpendingLockRepository {

    private final SpendingLockJpaRepository jpaRepository;

    @Override
    public void acquireLock(UUID agentId) {
        jpaRepository.findByAgentIdForUpdate(agentId);
    }

    @Override
    public void createIfNotExists(UUID agentId) {
        jpaRepository.insertIfNotExists(agentId);
    }
}
