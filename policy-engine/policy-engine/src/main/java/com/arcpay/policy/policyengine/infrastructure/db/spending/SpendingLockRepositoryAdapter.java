package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SpendingLockRepositoryAdapter implements SpendingLockRepository {

    private final SpendingLockJpaRepository jpaRepository;

    @Override
    public void acquireLock(UUID agentId) {
        createIfNotExists(agentId);
        jpaRepository.findByAgentIdForUpdate(agentId);
    }

    @Override
    public void createIfNotExists(UUID agentId) {
        try {
            var entity = SpendingLockEntity.builder()
                    .agentId(agentId)
                    .createdAt(Instant.now())
                    .build();
            jpaRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // Lock already exists — safe to ignore
        }
    }
}
