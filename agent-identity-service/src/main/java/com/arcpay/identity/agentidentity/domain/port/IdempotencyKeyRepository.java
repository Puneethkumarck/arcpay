package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.IdempotencyKey;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository {

    IdempotencyKey save(IdempotencyKey idempotencyKey);

    Optional<IdempotencyKey> findByKeyAndOwnerId(UUID idempotencyKey, UUID ownerId);

    void deleteExpiredBefore(Instant cutoff);
}
