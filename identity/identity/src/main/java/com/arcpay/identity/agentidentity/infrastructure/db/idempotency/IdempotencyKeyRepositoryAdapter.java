package com.arcpay.identity.agentidentity.infrastructure.db.idempotency;

import com.arcpay.identity.agentidentity.domain.model.IdempotencyKey;
import com.arcpay.identity.agentidentity.domain.port.IdempotencyKeyRepository;
import com.arcpay.identity.agentidentity.infrastructure.db.idempotency.mapper.IdempotencyKeyEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class IdempotencyKeyRepositoryAdapter implements IdempotencyKeyRepository {

    private final IdempotencyKeyJpaRepository jpaRepository;
    private final IdempotencyKeyEntityMapper mapper;

    @Override
    public IdempotencyKey save(IdempotencyKey idempotencyKey) {
        var entity = mapper.mapToEntity(idempotencyKey);
        return mapper.mapToDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<IdempotencyKey> findByKeyAndOwnerId(UUID idempotencyKey, UUID ownerId) {
        return jpaRepository.findByIdempotencyKeyAndOwnerId(idempotencyKey, ownerId)
                .map(mapper::mapToDomain);
    }

    @Override
    public void deleteExpiredBefore(Instant cutoff) {
        jpaRepository.deleteByExpiresAtBefore(cutoff);
    }
}
