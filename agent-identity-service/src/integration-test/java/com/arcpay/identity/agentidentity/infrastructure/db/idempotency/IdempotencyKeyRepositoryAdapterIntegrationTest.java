package com.arcpay.identity.agentidentity.infrastructure.db.idempotency;

import com.arcpay.identity.agentidentity.domain.port.IdempotencyKeyRepository;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY_EXPIRED;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_IDEMPOTENCY_KEY_ID;
import static com.arcpay.identity.agentidentity.fixtures.IdempotencyKeyFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM idempotency_keys");
        jdbcTemplate.update("DELETE FROM owners");
        insertOwner(SOME_OWNER_ID, "alice@example.com", "0x1111111111111111111111111111111111111111");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM idempotency_keys");
        jdbcTemplate.update("DELETE FROM owners");
    }

    @Test
    void shouldRoundTripSaveAndFindByKeyAndOwnerId() {
        // given
        idempotencyKeyRepository.save(SOME_IDEMPOTENCY_KEY);

        // when
        var result = idempotencyKeyRepository.findByKeyAndOwnerId(SOME_IDEMPOTENCY_KEY_ID, SOME_OWNER_ID);

        // then
        assertThat(result)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .isEqualTo(SOME_IDEMPOTENCY_KEY);
    }

    @Test
    void shouldReturnEmptyForNonExistentKey() {
        // given
        var nonExistentKey = UUID.fromString("0197c000-9999-7abc-8000-000000000999");

        // when
        var result = idempotencyKeyRepository.findByKeyAndOwnerId(nonExistentKey, SOME_OWNER_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldDeleteExpiredEntries() {
        // given
        idempotencyKeyRepository.save(SOME_IDEMPOTENCY_KEY);
        idempotencyKeyRepository.save(SOME_IDEMPOTENCY_KEY_EXPIRED);
        var cutoff = Instant.parse("2026-05-31T00:00:00Z");

        // when
        idempotencyKeyRepository.deleteExpiredBefore(cutoff);

        // then
        assertThat(idempotencyKeyRepository.findByKeyAndOwnerId(
                SOME_IDEMPOTENCY_KEY.idempotencyKey(), SOME_OWNER_ID))
                .isPresent();
        assertThat(idempotencyKeyRepository.findByKeyAndOwnerId(
                SOME_IDEMPOTENCY_KEY_EXPIRED.idempotencyKey(), SOME_OWNER_ID))
                .isEmpty();
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
