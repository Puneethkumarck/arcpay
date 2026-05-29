package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class IdentityServiceAdapter implements AgentServiceClient {

    private final IdentityServiceClient identityClient;
    private final CircuitBreaker identityCircuitBreaker;

    @Override
    public Optional<AgentInfo> resolveApiKey(String apiKeyHash) {
        try {
            return identityCircuitBreaker.executeSupplier(() -> {
                try {
                    return identityClient.resolveApiKey(apiKeyHash)
                            .map(r -> new AgentInfo(r.ownerId(), r.ownerId(), null, null));
                } catch (FeignException.NotFound e) {
                    log.debug("API key not found for hash={}", apiKeyHash);
                    return Optional.<AgentInfo>empty();
                } catch (FeignException e) {
                    throw new IdentityServiceUnavailableException(
                            "Identity service call failed: " + e.getMessage(), e);
                }
            });
        } catch (CallNotPermittedException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service circuit breaker is open", e);
        }
    }

    @Override
    public Optional<AgentInfo> getAgent(UUID agentId) {
        try {
            return identityCircuitBreaker.executeSupplier(() -> {
                try {
                    var response = identityClient.getAgent(agentId);
                    return Optional.of(new AgentInfo(
                            response.agentId(),
                            response.ownerId(),
                            response.status() != null ? response.status().name() : null,
                            response.policyHash()));
                } catch (FeignException.NotFound e) {
                    log.debug("Agent not found agentId={}", agentId);
                    return Optional.<AgentInfo>empty();
                } catch (FeignException e) {
                    throw new IdentityServiceUnavailableException(
                            "Identity service call failed: " + e.getMessage(), e);
                }
            });
        } catch (CallNotPermittedException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service circuit breaker is open", e);
        }
    }

    @Override
    public void updatePolicy(UUID agentId, String policyHash) {
        try {
            identityCircuitBreaker.executeSupplier(() -> {
                try {
                    var request = new UpdateAgentPolicyRequest(policyHash);
                    return identityClient.updatePolicy(agentId, request);
                } catch (FeignException e) {
                    throw new IdentityServiceUnavailableException(
                            "Identity service call failed: " + e.getMessage(), e);
                }
            });
        } catch (CallNotPermittedException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service circuit breaker is open", e);
        }
    }
}
