package com.arcpay.policy.policyengine.domain.agent;

import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_AGENT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_OWNED_BY_OTHER;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUSPENDED_AGENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AgentAuthorizationTest {

    @Mock
    private AgentServiceClient agentServiceClient;

    @InjectMocks
    private AgentAuthorization agentAuthorization;

    @Test
    void shouldReturnAgentWhenOwnershipVerified() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));

        // when
        var result = agentAuthorization.verifyOwnership(SOME_AGENT_ID, SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_AGENT);
    }

    @Test
    void shouldThrowWhenAgentNotOwned() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_AGENT_OWNED_BY_OTHER));

        // when / then
        assertThatThrownBy(() -> agentAuthorization.verifyOwnership(SOME_AGENT_ID, SOME_OWNER_ID))
                .isInstanceOf(AgentOwnershipException.class);
    }

    @Test
    void shouldThrowWhenAgentNotFound() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> agentAuthorization.verifyOwnership(SOME_AGENT_ID, SOME_OWNER_ID))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldReturnAgentWhenOwnershipAndActiveVerified() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));

        // when
        var result = agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_AGENT);
    }

    @Test
    void shouldThrowWhenAgentNotActive() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_SUSPENDED_AGENT));

        // when / then
        assertThatThrownBy(() -> agentAuthorization.verifyOwnershipAndActive(SOME_AGENT_ID, SOME_OWNER_ID))
                .isInstanceOf(AgentNotActiveException.class);
    }
}
