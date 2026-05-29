package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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

import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_AGENT_ID;
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
    private AgentServiceClient agentServiceClient;

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
    void shouldRegisterCircuitBreakerForIdentityClient() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(500)));
        assertThatThrownBy(() -> agentServiceClient.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class);

        // when / then
        var names = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .map(CircuitBreaker::getName)
                .toList();
        assertThat(names).anyMatch(n -> n.startsWith("IdentityServiceClient"));
    }

    @Test
    void shouldOpenCircuitAfterRepeatedServerErrorsAndMapToUnavailable() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(500)));

        // when
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> agentServiceClient.getAgent(SOME_AGENT_ID))
                    .isInstanceOf(IdentityServiceUnavailableException.class);
        }

        // then
        var breaker = identityBreaker();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> agentServiceClient.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service call failed");
    }

    @Test
    void shouldTimeOutSlowCallAndMapToUnavailable() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3_000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        // when / then
        assertThatThrownBy(() -> agentServiceClient.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service call failed");
    }

    @Test
    void shouldSurfaceNotFoundWithoutOpeningCircuit() {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(404)));

        // when
        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> agentServiceClient.getAgent(SOME_AGENT_ID))
                    .isInstanceOf(AgentNotFoundException.class)
                    .hasMessageContaining(SOME_AGENT_ID.toString());
        }

        // then
        var breaker = getAgentBreaker();
        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();

        identityServer.resetAll();
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(agentJson())));

        assertThat(agentServiceClient.getAgent(SOME_AGENT_ID)).isPresent();
    }

    private static String agentJson() {
        return "{"
                + "\"agentId\":\"" + SOME_AGENT_ID + "\","
                + "\"ownerId\":\"019576a0-0000-7000-8000-000000000003\","
                + "\"status\":\"ACTIVE\","
                + "\"policyHash\":\"0xabc123def456\","
                + "\"name\":\"test-agent\","
                + "\"createdAt\":\"2026-01-01T00:00:00Z\""
                + "}";
    }

    private CircuitBreaker identityBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("IdentityServiceClient")
                        && b.getMetrics().getNumberOfBufferedCalls() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No IdentityServiceClient circuit breaker recorded any calls — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }

    private CircuitBreaker getAgentBreaker() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("IdentityServiceClient")
                        && b.getName().contains("getAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No IdentityServiceClient getAgent circuit breaker was registered — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }
}
