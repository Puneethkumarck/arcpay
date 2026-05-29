package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignApiKeyResolver implements ApiKeyResolver {

    private final IdentityServiceClient identityClient;

    @Cacheable(value = "apiKeyResolution", key = "#apiKeyHash")
    @Override
    public Optional<OwnerPrincipal> resolve(String apiKeyHash) {
        try {
            return identityClient.resolveApiKey(apiKeyHash)
                    .map(r -> new OwnerPrincipal(r.ownerId(), r.email()));
        } catch (NoFallbackAvailableException e) {
            return failClosed(e.getCause() != null ? e.getCause() : e);
        } catch (CallNotPermittedException | FeignException e) {
            return failClosed(e);
        }
    }

    private Optional<OwnerPrincipal> failClosed(Throwable cause) {
        switch (cause) {
            case FeignException.NotFound ignored ->
                    log.debug("API key hash did not resolve to any owner");
            case CallNotPermittedException ignored ->
                    log.warn("Identity Service circuit breaker open — failing API key resolution closed");
            case FeignException feign ->
                    log.warn("Identity Service call failed during API key resolution — failing closed (status={})",
                            feign.status());
            default ->
                    log.warn("Identity Service call failed during API key resolution — failing closed", cause);
        }
        return Optional.empty();
    }
}
