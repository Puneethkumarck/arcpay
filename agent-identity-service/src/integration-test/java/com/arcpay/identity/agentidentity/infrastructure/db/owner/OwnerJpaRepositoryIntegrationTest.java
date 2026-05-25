package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

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
                .isEqualTo(SOME_OWNER_ENTITY);
    }

    @Test
    void shouldReturnTrueForExistingEmailCaseInsensitive() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);

        // when
        var result = jpaRepository.existsByEmailIgnoreCase(SOME_EMAIL.toUpperCase());

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueForExistingWalletAddressCaseInsensitive() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);

        // when
        var result = jpaRepository.existsByWalletAddressIgnoreCase(SOME_WALLET_ADDRESS.toUpperCase());

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // given
        jpaRepository.saveAndFlush(SOME_OWNER_ENTITY);
        var duplicate = SOME_OWNER_ENTITY.toBuilder()
                .ownerId(UUID.randomUUID())
                .email(SOME_EMAIL.toUpperCase())
                .walletAddress("0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .apiKeyHash("b".repeat(64))
                .build();

        // when / then
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
