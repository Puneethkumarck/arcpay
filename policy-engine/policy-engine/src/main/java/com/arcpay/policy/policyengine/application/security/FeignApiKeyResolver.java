package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FeignApiKeyResolver implements ApiKeyResolver {

    private final IdentityServiceClient identityClient;

    @Cacheable(value = "apiKeyResolution", key = "#apiKeyHash")
    @Override
    public Optional<OwnerPrincipal> resolve(String apiKeyHash) {
        return identityClient.resolveApiKey(apiKeyHash)
                .map(r -> new OwnerPrincipal(r.ownerId(), r.email()));
    }
}
