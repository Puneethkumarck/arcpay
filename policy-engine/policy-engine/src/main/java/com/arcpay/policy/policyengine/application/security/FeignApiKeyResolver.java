package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignApiKeyResolver implements ApiKeyResolver {

    private final IdentityServiceClient identityClient;

    /**
     * Resolves an API-key hash to an {@link OwnerPrincipal} via the Identity Service.
     *
     * <p>Fail-closed: any inability to positively resolve the key (404, circuit open,
     * server error) yields {@link Optional#empty()}, which the {@code ApiKeyAuthFilter}
     * treats as an authentication failure (→ 401). No raw exception is allowed to escape.
     */
    @Cacheable(value = "apiKeyResolution", key = "#apiKeyHash")
    @Override
    public Optional<OwnerPrincipal> resolve(String apiKeyHash) {
        try {
            return identityClient.resolveApiKey(apiKeyHash)
                    .map(r -> new OwnerPrincipal(r.ownerId(), r.email()));
        } catch (FeignException.NotFound e) {
            log.debug("API key hash did not resolve to any owner");
            return Optional.empty();
        } catch (CallNotPermittedException e) {
            log.warn("Identity Service circuit breaker open — failing API key resolution closed");
            return Optional.empty();
        } catch (FeignException e) {
            log.warn("Identity Service call failed during API key resolution — failing closed (status={})",
                    e.status());
            return Optional.empty();
        }
    }
}
