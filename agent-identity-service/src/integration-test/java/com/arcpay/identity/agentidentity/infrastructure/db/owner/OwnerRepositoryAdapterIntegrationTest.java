package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_API_KEY_HASH;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_CHECKSUMMED_WALLET;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_EMAIL;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OwnerRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private OwnerRepository ownerRepository;

    @Test
    void shouldRoundTripOwnerSaveAndFindById() {
        // given
        ownerRepository.save(SOME_OWNER);

        // when
        var result = ownerRepository.findById(SOME_OWNER_ID);

        // then
        assertThat(result)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(SOME_OWNER);
    }

    @Test
    void shouldFindOwnerByApiKeyHash() {
        // given
        ownerRepository.save(SOME_OWNER);

        // when
        var result = ownerRepository.findByApiKeyHash(SOME_API_KEY_HASH);

        // then
        assertThat(result)
                .isPresent()
                .get()
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(SOME_OWNER);
    }

    @Test
    void shouldReturnTrueWhenEmailExistsCaseInsensitive() {
        // given
        ownerRepository.save(SOME_OWNER);

        // when
        var result = ownerRepository.existsByEmailIgnoreCase(SOME_EMAIL.toUpperCase());

        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnTrueWhenWalletAddressExistsForChecksummedInput() {
        // given
        ownerRepository.save(SOME_OWNER);

        // when
        var result = ownerRepository.existsByWalletAddressIgnoreCase(SOME_CHECKSUMMED_WALLET);

        // then
        assertThat(result).isTrue();
    }

}
