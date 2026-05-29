package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class IdentityServiceAdapterTest {

    @Mock
    private IdentityServiceClient identityClient;

    private IdentityServiceAdapter adapter;

    private static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    private static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000003");
    private static final String SOME_POLICY_HASH = "0xabc123def456";

    @BeforeEach
    void setUp() {
        var circuitBreaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                .circuitBreaker("test-identity-service");
        adapter = new IdentityServiceAdapter(identityClient, circuitBreaker);
    }

    @Test
    void shouldGetAgent() {
        // given
        var response = AgentResponse.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .status(AgentStatusEnum.ACTIVE)
                .policyHash(SOME_POLICY_HASH)
                .name("test-agent")
                .createdAt(Instant.now())
                .build();
        given(identityClient.getAgent(SOME_AGENT_ID)).willReturn(response);

        // when
        var result = adapter.getAgent(SOME_AGENT_ID);

        // then
        var expected = Optional.of(new AgentInfo(
                SOME_AGENT_ID,
                SOME_OWNER_ID,
                AgentStatusEnum.ACTIVE.name(),
                SOME_POLICY_HASH));
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyWhenAgentNotFound() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID))
                .willThrow(feignNotFound());

        // when
        var result = adapter.getAgent(SOME_AGENT_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowIdentityServiceUnavailableWhenCircuitBreakerOpen() {
        // given
        var config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .build();
        var openCircuitBreaker = CircuitBreakerRegistry.of(config)
                .circuitBreaker("open-test");

        // Force the circuit breaker to open by recording failures
        openCircuitBreaker.transitionToOpenState();

        var adapterWithOpenCb = new IdentityServiceAdapter(identityClient, openCircuitBreaker);

        // when / then
        assertThatThrownBy(() -> adapterWithOpenCb.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");
    }

    @Test
    void shouldThrowIdentityServiceUnavailableOnConnectionFailure() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID))
                .willThrow(feignServerError());

        // when / then
        assertThatThrownBy(() -> adapter.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service call failed");
    }

    @Test
    void shouldUpdatePolicy() {
        // given
        var expectedRequest = new UpdateAgentPolicyRequest(SOME_POLICY_HASH);
        var response = AgentResponse.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .status(AgentStatusEnum.ACTIVE)
                .policyHash(SOME_POLICY_HASH)
                .build();
        given(identityClient.updatePolicy(SOME_AGENT_ID, expectedRequest)).willReturn(response);

        // when
        adapter.updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);

        // then
        then(identityClient).should().updatePolicy(SOME_AGENT_ID, expectedRequest);
    }

    private static FeignException.NotFound feignNotFound() {
        var request = Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("Not Found", request, null, Collections.emptyMap());
    }

    private static FeignException feignServerError() {
        var request = Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new FeignException.InternalServerError("Internal Server Error",
                request, null, Collections.emptyMap());
    }
}
