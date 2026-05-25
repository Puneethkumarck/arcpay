package com.arcpay.identity.agentidentity.infrastructure.db.idempotency;

import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_ENDPOINT;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY_EXPIRED;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_OWNER_ID;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_RESPONSE_BODY;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyJpaRepositoryIntegrationTest extends FullContextIntegrationTest {

    private static final UUID OTHER_OWNER_ID = UUID.fromString("019718a0-9999-7def-8000-fedcba654321");

    @Autowired
    private IdempotencyKeyJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM idempotency_keys");
        jdbcTemplate.update("DELETE FROM owners");
        insertOwner(SOME_OWNER_ID, "alice@example.com", "0x1111111111111111111111111111111111111111");
        insertOwner(OTHER_OWNER_ID, "bob@example.com", "0x2222222222222222222222222222222222222222");
    }

    @Test
    void shouldSaveAndFindByCompositeKey() {
        // given
        var entity = IdempotencyKeyEntity.builder()
                .idempotencyKey(SOME_IDEMPOTENCY_KEY.idempotencyKey())
                .ownerId(SOME_IDEMPOTENCY_KEY.ownerId())
                .endpoint(SOME_IDEMPOTENCY_KEY.endpoint())
                .responseStatus(SOME_IDEMPOTENCY_KEY.responseStatus())
                .responseBody(SOME_IDEMPOTENCY_KEY.responseBody())
                .createdAt(SOME_IDEMPOTENCY_KEY.createdAt())
                .expiresAt(SOME_IDEMPOTENCY_KEY.expiresAt())
                .build();

        // when
        repository.save(entity);
        var found = repository.findByIdempotencyKeyAndOwnerId(
                SOME_IDEMPOTENCY_KEY.idempotencyKey(),
                SOME_IDEMPOTENCY_KEY.ownerId());

        // then
        var expected = IdempotencyKeyEntity.builder()
                .idempotencyKey(SOME_IDEMPOTENCY_KEY.idempotencyKey())
                .ownerId(SOME_IDEMPOTENCY_KEY.ownerId())
                .endpoint(SOME_IDEMPOTENCY_KEY.endpoint())
                .responseStatus(SOME_IDEMPOTENCY_KEY.responseStatus())
                .responseBody(SOME_IDEMPOTENCY_KEY.responseBody())
                .createdAt(SOME_IDEMPOTENCY_KEY.createdAt())
                .expiresAt(SOME_IDEMPOTENCY_KEY.expiresAt())
                .build();
        assertThat(found).isPresent();
        assertThat(found.get())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldTreatSameKeyForDifferentOwnersAsSeparate() {
        // given
        var sharedKey = UUID.fromString("0197b000-1111-7abc-8000-000000000099");
        var entityForOwnerA = IdempotencyKeyEntity.builder()
                .idempotencyKey(sharedKey)
                .ownerId(SOME_OWNER_ID)
                .endpoint(SOME_ENDPOINT)
                .responseStatus(201)
                .responseBody(SOME_RESPONSE_BODY)
                .createdAt(Instant.parse("2026-06-01T10:00:00Z"))
                .expiresAt(Instant.parse("2026-06-02T10:00:00Z"))
                .build();
        var entityForOwnerB = IdempotencyKeyEntity.builder()
                .idempotencyKey(sharedKey)
                .ownerId(OTHER_OWNER_ID)
                .endpoint(SOME_ENDPOINT)
                .responseStatus(409)
                .responseBody("{\"error\":\"different\"}")
                .createdAt(Instant.parse("2026-06-01T11:00:00Z"))
                .expiresAt(Instant.parse("2026-06-02T11:00:00Z"))
                .build();

        // when
        repository.save(entityForOwnerA);
        repository.save(entityForOwnerB);
        var foundA = repository.findByIdempotencyKeyAndOwnerId(sharedKey, SOME_OWNER_ID);
        var foundB = repository.findByIdempotencyKeyAndOwnerId(sharedKey, OTHER_OWNER_ID);

        // then
        assertThat(foundA).isPresent();
        assertThat(foundA.get())
                .usingRecursiveComparison()
                .isEqualTo(entityForOwnerA);
        assertThat(foundB).isPresent();
        assertThat(foundB.get())
                .usingRecursiveComparison()
                .isEqualTo(entityForOwnerB);
    }

    @Test
    void shouldDeleteExpiredEntries() {
        // given
        var active = IdempotencyKeyEntity.builder()
                .idempotencyKey(SOME_IDEMPOTENCY_KEY.idempotencyKey())
                .ownerId(SOME_IDEMPOTENCY_KEY.ownerId())
                .endpoint(SOME_IDEMPOTENCY_KEY.endpoint())
                .responseStatus(SOME_IDEMPOTENCY_KEY.responseStatus())
                .responseBody(SOME_IDEMPOTENCY_KEY.responseBody())
                .createdAt(SOME_IDEMPOTENCY_KEY.createdAt())
                .expiresAt(SOME_IDEMPOTENCY_KEY.expiresAt())
                .build();
        var expired = IdempotencyKeyEntity.builder()
                .idempotencyKey(SOME_IDEMPOTENCY_KEY_EXPIRED.idempotencyKey())
                .ownerId(SOME_IDEMPOTENCY_KEY_EXPIRED.ownerId())
                .endpoint(SOME_IDEMPOTENCY_KEY_EXPIRED.endpoint())
                .responseStatus(SOME_IDEMPOTENCY_KEY_EXPIRED.responseStatus())
                .responseBody(SOME_IDEMPOTENCY_KEY_EXPIRED.responseBody())
                .createdAt(SOME_IDEMPOTENCY_KEY_EXPIRED.createdAt())
                .expiresAt(SOME_IDEMPOTENCY_KEY_EXPIRED.expiresAt())
                .build();
        repository.save(active);
        repository.save(expired);
        var cutoff = Instant.parse("2026-05-31T00:00:00Z");

        // when
        repository.deleteByExpiresAtBefore(cutoff);

        // then
        assertThat(repository.findByIdempotencyKeyAndOwnerId(active.getIdempotencyKey(), active.getOwnerId()))
                .isPresent();
        assertThat(repository.findByIdempotencyKeyAndOwnerId(expired.getIdempotencyKey(), expired.getOwnerId()))
                .isEmpty();
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        // given
        var nonExistentKey = UUID.fromString("0197c000-9999-7abc-8000-000000000999");

        // when
        var result = repository.findByIdempotencyKeyAndOwnerId(nonExistentKey, SOME_OWNER_ID);

        // then
        assertThat(result).isEmpty();
    }

    private void insertOwner(UUID ownerId, String email, String walletAddress) {
        var now = Timestamp.from(Instant.parse("2026-06-01T09:00:00Z"));
        jdbcTemplate.update(
                "INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status, created_at, updated_at) " +
                        "VALUES (?::uuid, ?, ?, ?, ?, ?, ?)",
                ownerId.toString(),
                email,
                walletAddress,
                "hash-" + ownerId,
                "ACTIVE",
                now,
                now);
    }
}
