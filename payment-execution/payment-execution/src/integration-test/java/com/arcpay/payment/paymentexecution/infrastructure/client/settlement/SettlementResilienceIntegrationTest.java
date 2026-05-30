package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import com.arcpay.payment.paymentexecution.domain.exception.SettlementServiceUnavailableException;
import com.arcpay.payment.paymentexecution.domain.port.SettlementPort;
import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import feign.FeignException;
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

import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.TRANSFERS_PATH;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.balancePath;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubBalance;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferAccepted;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferClientError;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferServerError;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
class SettlementResilienceIntegrationTest extends FullContextIntegrationTest {

    private static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    private static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    private static final String SOME_WALLET_ID = "circle-wallet-abc";
    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");

    private static WireMockServer settlementServer;

    @Autowired
    private SettlementPort settlementPort;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startSettlementStub() {
        settlementServer = new WireMockServer(0);
        settlementServer.start();
    }

    @AfterAll
    static void stopSettlementStub() {
        if (settlementServer != null) {
            settlementServer.stop();
        }
    }

    @DynamicPropertySource
    static void settlementProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.settlement-service.url", () -> "http://localhost:" + settlementServer.port());
    }

    @BeforeEach
    void resetState() {
        settlementServer.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Test
    void shouldReturnTxIdAndSendServiceAuthOnTransfer() {
        // given
        stubTransferAccepted(settlementServer, SOME_PAYMENT_ID);

        // when
        var txId = settlementPort.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT);

        // then
        assertThat(txId).isEqualTo("ctx-abc-123");
        settlementServer.verify(postRequestedFor(urlPathEqualTo(TRANSFERS_PATH))
                .withHeader("X-Service-Auth", equalTo("test-service-token")));
    }

    @Test
    void shouldReturnBalanceAndSendServiceAuth() {
        // given
        stubBalance(settlementServer, SOME_AGENT_ID, "100.00");

        // when
        var balance = settlementPort.balance(SOME_AGENT_ID);

        // then
        assertThat(balance).isEqualByComparingTo(new BigDecimal("100.00"));
        settlementServer.verify(getRequestedFor(urlPathEqualTo(balancePath(SOME_AGENT_ID)))
                .withHeader("X-Service-Auth", equalTo("test-service-token")));
    }

    @Test
    void shouldOpenCircuitAfterRepeatedServerErrorsAndFailClosed() {
        // given
        stubTransferServerError(settlementServer);

        // when
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> settlementPort.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT))
                    .isInstanceOf(SettlementServiceUnavailableException.class);
        }

        // then
        assertThat(transferBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> settlementPort.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .isInstanceOf(SettlementServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");
    }

    @Test
    void shouldPropagateClientErrorWithoutTrippingBreaker() {
        // given
        stubTransferClientError(settlementServer);

        // when
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> settlementPort.transfer(SOME_PAYMENT_ID, SOME_WALLET_ID, SOME_RECIPIENT, SOME_AMOUNT))
                    .isInstanceOf(FeignException.class)
                    .isNotInstanceOf(SettlementServiceUnavailableException.class);
        }

        // then
        var anyTransferBreakerOpen = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("SettlementServiceClient"))
                .anyMatch(b -> b.getState() == CircuitBreaker.State.OPEN);
        assertThat(anyTransferBreakerOpen).isFalse();
    }

    private CircuitBreaker transferBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("SettlementServiceClient")
                        && b.getMetrics().getNumberOfBufferedCalls() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No SettlementServiceClient circuit breaker recorded any calls — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }
}
