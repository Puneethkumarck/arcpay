package com.arcpay.payment.paymentexecution.infrastructure.client.policy;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.payment.paymentexecution.domain.exception.PolicyServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.port.PolicyPort;
import com.arcpay.policy.client.PolicyEngineCallException;
import com.arcpay.policy.client.PolicyEngineClient;
import com.arcpay.policy.policyengine.api.model.ReserveRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
class PolicyServiceAdapter implements PolicyPort {

    private final PolicyEngineClient policyClient;
    private final PolicyResultMapper policyResultMapper;

    @Override
    public PolicyResult reserve(UUID paymentId, UUID agentId, String recipientAddress, BigDecimal amount) {
        try {
            var request = ReserveRequest.builder()
                    .paymentId(paymentId)
                    .agentId(agentId)
                    .recipientAddress(recipientAddress)
                    .amount(amount)
                    .requestedAt(Instant.now())
                    .build();
            return policyResultMapper.toDomain(policyClient.reserve(request));
        } catch (PolicyEngineCallException e) {
            throw toUnavailable("reserve", paymentId, e);
        }
    }

    @Override
    public void commit(UUID paymentId) {
        try {
            policyClient.commit(paymentId);
        } catch (PolicyEngineCallException e) {
            throw toUnavailable("commit", paymentId, e);
        }
    }

    @Override
    public void release(UUID paymentId) {
        try {
            policyClient.release(paymentId);
        } catch (PolicyEngineCallException e) {
            throw toUnavailable("release", paymentId, e);
        }
    }

    private PolicyServiceUnavailableException toUnavailable(String operation, UUID paymentId, PolicyEngineCallException e) {
        var reason = switch (e.getCause()) {
            case CallNotPermittedException _ -> "Policy service circuit breaker is open";
            case TimeoutException _ -> "Policy service call timed out";
            case null, default -> "Policy service call failed";
        };
        return new PolicyServiceUnavailableException(
                reason + " during " + operation + " for payment " + paymentId, e);
    }
}
