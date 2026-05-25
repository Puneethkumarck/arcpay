package com.arcpay.identity.agentidentity.infrastructure.db.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyId> {

    Optional<IdempotencyKeyEntity> findByIdempotencyKeyAndOwnerId(UUID idempotencyKey, UUID ownerId);

    @Modifying
    @Transactional
    void deleteByExpiresAtBefore(Instant cutoff);
}
