package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
class IdentityServiceAdapter implements AgentServiceClient {

    private final IdentityServiceClient identityClient;
    private final AgentInfoMapper agentInfoMapper;

    @Override
    public Optional<AgentInfo> getAgent(UUID agentId) {
        try {
            var response = guard(() -> identityClient.getAgent(agentId));
            return Optional.of(agentInfoMapper.toDomain(response));
        } catch (FeignException.NotFound e) {
            log.debug("Agent not found in Identity Service");
            throw new AgentNotFoundException(agentId);
        }
    }

    @Override
    public void updatePolicy(UUID agentId, String policyHash) {
        var request = new UpdateAgentPolicyRequest(policyHash);
        guard(() -> identityClient.updatePolicy(agentId, request));
    }

    private <T> T guard(Supplier<T> call) {
        try {
            return call.get();
        } catch (NoFallbackAvailableException e) {
            throw mapCircuitBreakerFailure(unwrap(e));
        } catch (CallNotPermittedException e) {
            throw circuitOpen(e);
        } catch (FeignException.NotFound e) {
            throw e;
        } catch (FeignException e) {
            throw callFailed(e);
        }
    }

    private RuntimeException mapCircuitBreakerFailure(Throwable cause) {
        return switch (cause) {
            case FeignException.NotFound notFound -> notFound;
            case CallNotPermittedException cnp -> circuitOpen(cnp);
            case TimeoutException timeout -> timedOut(timeout);
            case FeignException feign -> callFailed(feign);
            default -> new IdentityServiceUnavailableException("Identity service call failed", cause);
        };
    }

    private static Throwable unwrap(NoFallbackAvailableException e) {
        return e.getCause() != null ? e.getCause() : e;
    }

    private static IdentityServiceUnavailableException circuitOpen(Throwable cause) {
        return new IdentityServiceUnavailableException("Identity service circuit breaker is open", cause);
    }

    private static IdentityServiceUnavailableException timedOut(Throwable cause) {
        return new IdentityServiceUnavailableException("Identity service call timed out", cause);
    }

    private static IdentityServiceUnavailableException callFailed(Throwable cause) {
        return new IdentityServiceUnavailableException("Identity service call failed", cause);
    }
}
