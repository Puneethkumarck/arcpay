package com.arcpay.payment.paymentexecution.infrastructure.client.policy;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.payment.paymentexecution.domain.exception.PolicyServiceUnavailableException;
import com.arcpay.policy.client.PolicyEngineCallException;
import com.arcpay.policy.client.PolicyEngineClient;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PolicyServiceAdapterTest {

    private static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    private static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");

    @Mock
    private PolicyEngineClient policyClient;

    @Spy
    private PolicyResultMapper policyResultMapper = org.mapstruct.factory.Mappers.getMapper(PolicyResultMapper.class);

    @InjectMocks
    private PolicyServiceAdapter adapter;

    @Test
    void shouldMapReserveResponseToPolicyResult() {
        // given
        given(policyClient.reserve(org.mockito.ArgumentMatchers.argThat(
                req -> req.paymentId().equals(SOME_PAYMENT_ID)
                        && req.agentId().equals(SOME_AGENT_ID))))
                .willReturn(PolicyEvaluationResponse.builder().verdict("APPROVED").build());

        // when
        var result = adapter.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);

        // then
        var expected = PolicyResult.builder().verdict("APPROVED").rulesEvaluated(0).build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapOpenCircuitToUnavailableOnReserve() {
        // given
        var openCircuit = CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.of("policy", CircuitBreakerConfig.ofDefaults()));
        given(policyClient.reserve(org.mockito.ArgumentMatchers.any()))
                .willThrow(new PolicyEngineCallException("Policy service call failed", openCircuit));

        // when / then
        assertThatThrownBy(() -> adapter.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(PolicyServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open")
                .hasMessageContaining(SOME_PAYMENT_ID.toString());
    }

    @Test
    void shouldMapCallFailureToUnavailableOnCommit() {
        // given
        given(policyClient.commit(SOME_PAYMENT_ID))
                .willThrow(new PolicyEngineCallException("Policy service call failed", new RuntimeException("boom")));

        // when / then
        assertThatThrownBy(() -> adapter.commit(SOME_PAYMENT_ID))
                .isInstanceOf(PolicyServiceUnavailableException.class)
                .hasMessageContaining("during commit");
    }

    @Test
    void shouldMapCallFailureToUnavailableOnRelease() {
        // given
        given(policyClient.release(SOME_PAYMENT_ID))
                .willThrow(new PolicyEngineCallException("Policy service call failed", new RuntimeException("boom")));

        // when / then
        assertThatThrownBy(() -> adapter.release(SOME_PAYMENT_ID))
                .isInstanceOf(PolicyServiceUnavailableException.class)
                .hasMessageContaining("during release");
    }

    @Test
    void shouldDelegateCommitToClient() {
        // when
        adapter.commit(SOME_PAYMENT_ID);

        // then
        then(policyClient).should().commit(SOME_PAYMENT_ID);
    }
}
