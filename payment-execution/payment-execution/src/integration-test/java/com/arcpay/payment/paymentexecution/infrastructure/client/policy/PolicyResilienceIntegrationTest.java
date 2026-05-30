package com.arcpay.payment.paymentexecution.infrastructure.client.policy;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.payment.paymentexecution.domain.exception.PolicyServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.port.PolicyPort;
import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.RESERVE_PATH;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReserveApproved;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReserveServerError;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=3",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=3",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=30s",
        "resilience4j.timelimiter.configs.default.timeout-duration=2s"
})
class PolicyResilienceIntegrationTest extends FullContextIntegrationTest {

    private static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    private static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");

    private static WireMockServer policyServer;

    @Autowired
    private PolicyPort policyPort;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startPolicyStub() {
        policyServer = new WireMockServer(0);
        policyServer.start();
    }

    @AfterAll
    static void stopPolicyStub() {
        if (policyServer != null) {
            policyServer.stop();
        }
    }

    @DynamicPropertySource
    static void policyProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.policy-engine.url", () -> "http://localhost:" + policyServer.port());
    }

    @BeforeEach
    void resetState() {
        policyServer.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Test
    void shouldMapReserveSuccessToPolicyResultAndSendServiceAuth() {
        // given
        stubReserveApproved(policyServer);

        // when
        var result = policyPort.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);

        // then
        var expected = PolicyResult.builder().verdict("APPROVED").rulesEvaluated(1).build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        policyServer.verify(postRequestedFor(urlPathEqualTo(RESERVE_PATH))
                .withHeader("X-Service-Auth", equalTo("test-service-token")));
    }

    @Test
    void shouldOpenCircuitAfterRepeatedServerErrorsAndFailClosed() {
        // given
        stubReserveServerError(policyServer);

        // when
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> policyPort.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT))
                    .isInstanceOf(PolicyServiceUnavailableException.class);
        }

        // then
        assertThat(reserveBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> policyPort.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(PolicyServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");
    }

    private CircuitBreaker reserveBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("PolicyEngineClient")
                        && b.getMetrics().getNumberOfBufferedCalls() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No PolicyEngineClient circuit breaker recorded any calls — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }
}
