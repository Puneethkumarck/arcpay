package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_API_KEY_HASH;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IdentityApiKeyResolverTest {

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private IdentityApiKeyResolver identityApiKeyResolver;

    @Test
    void shouldResolveOwnerPrincipalForValidApiKeyHash() {
        // given
        given(ownerRepository.findByApiKeyHash(SOME_API_KEY_HASH)).willReturn(Optional.of(SOME_OWNER));
        var expected = new OwnerPrincipal(SOME_OWNER.ownerId(), SOME_OWNER.email());

        // when
        var result = identityApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void shouldReturnEmptyForUnknownApiKeyHash() {
        // given
        given(ownerRepository.findByApiKeyHash("unknown-hash")).willReturn(Optional.empty());

        // when
        var result = identityApiKeyResolver.resolve("unknown-hash");

        // then
        assertThat(result).isEmpty();
    }
}
