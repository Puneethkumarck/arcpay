package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class IdentityApiKeyResolver implements ApiKeyResolver {

    private final OwnerRepository ownerRepository;

    @Override
    public Optional<OwnerPrincipal> resolve(String apiKeyHash) {
        return ownerRepository.findByApiKeyHash(apiKeyHash)
                .map(owner -> new OwnerPrincipal(owner.ownerId(), owner.email()));
    }
}
