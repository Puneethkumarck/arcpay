package com.arcpay.compliance.infrastructure.client.identity;

import com.arcpay.compliance.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.compliance.domain.port.OwnerResolver;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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

import static com.arcpay.compliance.fixtures.CircuitBreakerFixtures.getAgentBreaker;
import static com.arcpay.compliance.fixtures.CircuitBreakerFixtures.identityCallBreaker;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.someAgentResponseJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.configs.default.sliding-window-size=3",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=3",
        "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=30s",
        "resilience4j.timelimiter.configs.default.timeout-duration=1s",
        "resilience4j.timelimiter.configs.default.cancel-running-future=true"
})
class IdentityResilienceIntegrationTest extends FullContextIntegrationTest {

    private static final String AGENT_PATH = "/api/v1/internal/agents/" + SOME_AGENT_ID;

    private static WireMockServer identityServer;

    @Autowired
    private OwnerResolver ownerResolver;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startIdentityStub() {
        identityServer = new WireMockServer(0);
        identityServer.start();
    }

    @AfterAll
    static void stopIdentityStub() {
        if (identityServer != null) {
            identityServer.stop();
        }
    }

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.identity-service.url", () -> "http://localhost:" + identityServer.port());
    }

    @BeforeEach
    void resetState() {
        identityServer.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Test
    void shouldResolveOwnerForAgent() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(someAgentResponseJson())));

        // when / then
        assertThat(ownerResolver.resolveOwner(SOME_AGENT_ID)).isEqualTo(SOME_OWNER_ID);
    }

    @Test
    void shouldOpenCircuitAfterRepeatedServerErrorsAndMapToUnavailable() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(500)));

        // when
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> ownerResolver.resolveOwner(SOME_AGENT_ID))
                    .isInstanceOf(IdentityServiceUnavailableException.class);
        }

        // then
        assertThat(identityCallBreaker(circuitBreakerRegistry).getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> ownerResolver.resolveOwner(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class);
    }

    @Test
    void shouldTimeOutSlowCallAndMapToUnavailable() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3_000)
                        .withHeader("Content-Type", "application/json")
                        .withBody(someAgentResponseJson())));

        // when / then
        assertThatThrownBy(() -> ownerResolver.resolveOwner(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class);
    }

    @Test
    void shouldSurfaceNotFoundWithoutOpeningCircuit() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(404)));

        // when
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> ownerResolver.resolveOwner(SOME_AGENT_ID))
                    .isInstanceOf(FeignException.NotFound.class);
        }

        // then
        var breaker = getAgentBreaker(circuitBreakerRegistry);
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }
}
