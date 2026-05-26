package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_API_KEY_HASH;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_EMAIL;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ENTITY;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_WALLET_ADDRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class OwnerJpaRepositoryIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private OwnerJpaRepository jpaRepository;

    @Test
    void shouldFindOwnerByApiKeyHash() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);

        // when
        var result = jpaRepository.findByApiKeyHash(SOME_API_KEY_HASH);

        // then
        assertThat(result)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(SOME_OWNER_ENTITY);
    }

    @Test
    void shouldReturnTrueForExistingEmailCaseInsensitive() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);

        // when
        var result = jpaRepository.existsByEmailIgnoreCase(SOME_EMAIL.toUpperCase(Locale.ROOT));

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForExistingWalletAddressCaseInsensitive() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);

        // when
        var result = jpaRepository.existsByWalletAddressIgnoreCase(SOME_WALLET_ADDRESS.toUpperCase(Locale.ROOT));

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);
        var duplicate = SOME_OWNER_ENTITY.toBuilder()
                .ownerId(UUID.randomUUID())
                .email(SOME_EMAIL.toUpperCase(Locale.ROOT))
                .walletAddress("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .apiKeyHash("b".repeat(64))
                .build();

        // when / then
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("idx_owners_email");
    }

    @Test
    void shouldRejectDuplicateWalletAddress() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);
        var duplicate = SOME_OWNER_ENTITY.toBuilder()
                .ownerId(UUID.randomUUID())
                .email("bob@example.com")
                .walletAddress(SOME_WALLET_ADDRESS.toUpperCase(Locale.ROOT))
                .apiKeyHash("c".repeat(64))
                .build();

        // when / then
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("idx_owners_wallet");
    }
}
