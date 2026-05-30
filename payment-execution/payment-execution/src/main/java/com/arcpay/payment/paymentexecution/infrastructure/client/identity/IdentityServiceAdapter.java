package com.arcpay.payment.paymentexecution.infrastructure.client.identity;

import com.arcpay.identity.client.IdentityServiceCallException;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.payment.paymentexecution.domain.exception.AgentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import com.arcpay.payment.paymentexecution.domain.port.AgentServiceClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
class IdentityServiceAdapter implements AgentServiceClient {

    private final IdentityServiceClient identityClient;
    private final AgentInfoMapper agentInfoMapper;

    @Override
    public Optional<AgentInfo> getAgent(UUID agentId) {
        try {
            var response = identityClient.getAgent(agentId);
            return Optional.of(agentInfoMapper.toDomain(response));
        } catch (FeignException.NotFound e) {
            log.debug("Agent not found in Identity Service agentId={}", agentId);
            throw new AgentNotFoundException(agentId);
        } catch (IdentityServiceCallException e) {
            throw toUnavailable(e);
        }
    }

    private IdentityServiceUnavailableException toUnavailable(IdentityServiceCallException e) {
        var message = switch (e.getCause()) {
            case CallNotPermittedException _ -> "Identity service circuit breaker is open";
            case TimeoutException _ -> "Identity service call timed out";
            case null, default -> "Identity service call failed";
        };
        return new IdentityServiceUnavailableException(message, e);
    }
}
