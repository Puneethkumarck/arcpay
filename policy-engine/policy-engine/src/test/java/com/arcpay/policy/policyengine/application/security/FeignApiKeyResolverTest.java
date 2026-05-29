package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_API_KEY_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_EMAIL;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_OWNER_PRINCIPAL_RESPONSE;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.feignNotFound;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.feignServerError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeignApiKeyResolverTest {

    @Mock
    private IdentityServiceClient identityClient;

    @InjectMocks
    private FeignApiKeyResolver feignApiKeyResolver;

    @Test
    void shouldResolveApiKeyAndReturnOwnerPrincipal() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.of(SOME_OWNER_PRINCIPAL_RESPONSE));

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        var expected = Optional.of(new OwnerPrincipal(SOME_OWNER_ID, SOME_EMAIL));
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.empty());

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenIdentityReturns404() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willThrow(feignNotFound());

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then — fail-closed: 404 yields empty (→ 401)
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenIdentityReturnsServerError() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willThrow(feignServerError());

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then — fail-closed: server error yields empty (→ 401)
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenCircuitBreakerOpen() {
        // given
        var openCircuitBreaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                .circuitBreaker("resolver-open-test");
        openCircuitBreaker.transitionToOpenState();
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willThrow(callNotPermitted(openCircuitBreaker));

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then — fail-closed: circuit open yields empty (→ 401)
        assertThat(result).isEmpty();
    }

    private static CallNotPermittedException callNotPermitted(CircuitBreaker circuitBreaker) {
        return CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
    }
}
