package com.arcpay.compliance.infrastructure.client.identity;

import com.arcpay.compliance.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.compliance.domain.port.OwnerResolver;
import com.arcpay.identity.client.IdentityServiceCallException;
import com.arcpay.identity.client.IdentityServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class IdentityServiceAdapter implements OwnerResolver {

    private final IdentityServiceClient identityClient;

    @Override
    public UUID resolveOwner(UUID agentId) {
        try {
            return identityClient.getAgent(agentId).ownerId();
        } catch (FeignException.NotFound e) {
            log.debug("Agent not found in Identity Service while resolving owner");
            throw e;
        } catch (IdentityServiceCallException e) {
            throw new IdentityServiceUnavailableException(agentId, e);
        }
    }
}
