package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class IdentityApiKeyResolver implements ApiKeyResolver {

    private final OwnerRepository ownerRepository;
    private final OwnerAuthorities ownerAuthorities;

    @Override
    public Optional<OwnerPrincipal> resolve(String apiKeyHash) {
        return ownerRepository
                .findByApiKeyHash(apiKeyHash)
                .map(owner ->
                        new OwnerPrincipal(owner.ownerId(), owner.email(), ownerAuthorities.forApiKeyHash(apiKeyHash)));
    }
}
