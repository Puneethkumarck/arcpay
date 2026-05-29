package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
@Slf4j
class IdentityServiceAdapter implements AgentServiceClient {

    private final IdentityServiceClient identityClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final AgentInfoMapper agentInfoMapper;
    private final ExecutorService executor;

    IdentityServiceAdapter(IdentityServiceClient identityClient,
                           CircuitBreaker identityCircuitBreaker,
                           TimeLimiter identityTimeLimiter,
                           AgentInfoMapper agentInfoMapper) {
        this.identityClient = identityClient;
        this.circuitBreaker = identityCircuitBreaker;
        this.timeLimiter = identityTimeLimiter;
        this.agentInfoMapper = agentInfoMapper;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public Optional<AgentInfo> getAgent(UUID agentId) {
        try {
            var response = call(() -> identityClient.getAgent(agentId));
            return Optional.of(agentInfoMapper.toDomain(response));
        } catch (FeignException.NotFound e) {
            log.debug("Agent not found in Identity Service");
            throw new AgentNotFoundException(agentId);
        }
    }

    @Override
    public void updatePolicy(UUID agentId, String policyHash) {
        var request = new UpdateAgentPolicyRequest(policyHash);
        call(() -> identityClient.updatePolicy(agentId, request));
    }

    /**
     * Decorates an Identity Service call with the circuit breaker and time limiter.
     *
     * <ul>
     *   <li>Circuit OPEN ({@link CallNotPermittedException}) → {@link IdentityServiceUnavailableException}</li>
     *   <li>Timeout ({@link TimeoutException}) → {@link IdentityServiceUnavailableException} (call is cancelled)</li>
     *   <li>Server-side {@link FeignException} → {@link IdentityServiceUnavailableException}</li>
     * </ul>
     *
     * {@link FeignException.NotFound} is rethrown so callers can map 404 to their own semantics.
     */
    private <T> T call(Supplier<T> supplier) {
        var decorated = circuitBreaker.decorateSupplier(supplier);
        try {
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(decorated, executor));
        } catch (FeignException.NotFound e) {
            throw e;
        } catch (CallNotPermittedException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service circuit breaker is open", e);
        } catch (TimeoutException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service call timed out", e);
        } catch (FeignException e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service call failed", e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof FeignException.NotFound notFound) {
                throw notFound;
            }
            if (e.getCause() instanceof CallNotPermittedException cnp) {
                throw new IdentityServiceUnavailableException(
                        "Identity service circuit breaker is open", cnp);
            }
            if (e.getCause() instanceof FeignException feign) {
                throw new IdentityServiceUnavailableException(
                        "Identity service call failed", feign);
            }
            throw new IdentityServiceUnavailableException(
                    "Identity service call failed", e);
        } catch (Exception e) {
            throw new IdentityServiceUnavailableException(
                    "Identity service call failed", e);
        }
    }
}
