package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_AGENT_RESPONSE;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_POLICY_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.feignNotFound;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.feignServerError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class IdentityServiceAdapterTest {

    @Mock
    private IdentityServiceClient identityClient;

    private final AgentInfoMapper agentInfoMapper = Mappers.getMapper(AgentInfoMapper.class);

    private IdentityServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        var circuitBreaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                .circuitBreaker("test-identity-service");
        var timeLimiter = TimeLimiterRegistry.of(
                        TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3)).build())
                .timeLimiter("test-identity-service");
        adapter = new IdentityServiceAdapter(identityClient, circuitBreaker, timeLimiter, agentInfoMapper);
    }

    @Test
    void shouldGetAgent() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willReturn(SOME_AGENT_RESPONSE);

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
    void shouldThrowAgentNotFoundWhenAgentMissing() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willThrow(feignNotFound());

        // when / then
        assertThatThrownBy(() -> adapter.getAgent(SOME_AGENT_ID))
                .isInstanceOf(AgentNotFoundException.class)
                .hasMessageContaining(SOME_AGENT_ID.toString());
    }

    @Test
    void shouldThrowIdentityServiceUnavailableWhenCircuitBreakerOpen() {
        // given
        var openCircuitBreaker = CircuitBreakerRegistry.ofDefaults()
                .circuitBreaker("open-test");
        openCircuitBreaker.transitionToOpenState();
        var timeLimiter = TimeLimiterRegistry.ofDefaults().timeLimiter("open-test");
        var adapterWithOpenCb = new IdentityServiceAdapter(
                identityClient, openCircuitBreaker, timeLimiter, agentInfoMapper);

        // when / then
        assertThatThrownBy(() -> adapterWithOpenCb.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");
    }

    @Test
    void shouldThrowIdentityServiceUnavailableOnServerError() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willThrow(feignServerError());

        // when / then
        assertThatThrownBy(() -> adapter.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("Identity service call failed");
    }

    @Test
    void shouldThrowIdentityServiceUnavailableWhenCallTimesOut() {
        // given
        var circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("timeout-test");
        var shortTimeLimiter = TimeLimiterRegistry.of(
                        TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofMillis(100))
                                .cancelRunningFuture(true)
                                .build())
                .timeLimiter("timeout-test");
        var slowAdapter = new IdentityServiceAdapter(
                identityClient, circuitBreaker, shortTimeLimiter, agentInfoMapper);
        given(identityClient.getAgent(SOME_AGENT_ID)).willAnswer(invocation -> {
            Thread.sleep(2_000);
            return SOME_AGENT_RESPONSE;
        });

        // when / then
        assertThatThrownBy(() -> slowAdapter.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void shouldUpdatePolicy() {
        // given
        var expectedRequest = new UpdateAgentPolicyRequest(SOME_POLICY_HASH);
        given(identityClient.updatePolicy(SOME_AGENT_ID, expectedRequest)).willReturn(SOME_AGENT_RESPONSE);

        // when
        adapter.updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);

        // then
        then(identityClient).should().updatePolicy(SOME_AGENT_ID, expectedRequest);
    }
}
