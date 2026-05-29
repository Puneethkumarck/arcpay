package com.arcpay.policy.policyengine.infrastructure.client.identity;

import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

/**
 * Unit test for {@link IdentityServiceAdapter}.
 *
 * <p>The circuit breaker + time limiter are now wired by the Spring Cloud OpenFeign
 * integration (verified end-to-end in {@code IdentityResilienceIntegrationTest}). These
 * tests cover only the adapter's exception-mapping contract by feeding it the exceptions
 * the integration surfaces from the mocked {@link IdentityServiceClient}.
 */
@ExtendWith(MockitoExtension.class)
class IdentityServiceAdapterTest {

    @Mock
    private IdentityServiceClient identityClient;

    private final AgentInfoMapper agentInfoMapper = Mappers.getMapper(AgentInfoMapper.class);

    private IdentityServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityServiceAdapter(identityClient, agentInfoMapper);
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
        // given — the OpenFeign integration surfaces CallNotPermittedException when the breaker is OPEN
        given(identityClient.getAgent(SOME_AGENT_ID)).willThrow(callNotPermitted());

        // when / then
        assertThatThrownBy(() -> adapter.getAgent(SOME_AGENT_ID))
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
    void shouldUpdatePolicy() {
        // given
        var expectedRequest = new UpdateAgentPolicyRequest(SOME_POLICY_HASH);
        given(identityClient.updatePolicy(SOME_AGENT_ID, expectedRequest)).willReturn(SOME_AGENT_RESPONSE);

        // when
        adapter.updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);

        // then
        then(identityClient).should().updatePolicy(SOME_AGENT_ID, expectedRequest);
    }

    @Test
    void shouldThrowIdentityServiceUnavailableWhenUpdatePolicyCircuitOpen() {
        // given
        var expectedRequest = new UpdateAgentPolicyRequest(SOME_POLICY_HASH);
        given(identityClient.updatePolicy(SOME_AGENT_ID, expectedRequest)).willThrow(callNotPermitted());

        // when / then
        assertThatThrownBy(() -> adapter.updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining("circuit breaker is open");
    }

    private static CallNotPermittedException callNotPermitted() {
        var breaker = CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults())
                .circuitBreaker("unit-test");
        breaker.transitionToOpenState();
        return CallNotPermittedException.createCallNotPermittedException(breaker);
    }
}
