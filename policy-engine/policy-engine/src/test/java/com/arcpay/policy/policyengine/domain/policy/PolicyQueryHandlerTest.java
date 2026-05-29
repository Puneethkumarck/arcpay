package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.domain.agent.AgentAuthorization;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUPERSEDED_POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class PolicyQueryHandlerTest {

    @Mock
    private AgentAuthorization agentAuthorization;

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyQueryHandler policyQueryHandler;

    @Test
    void shouldReturnActivePolicy() {
        // given
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_POLICY));

        // when
        var result = policyQueryHandler.getActivePolicy(SOME_AGENT_ID, SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_POLICY);
    }

    @Test
    void shouldThrowWhenNoActivePolicy() {
        // given
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> policyQueryHandler.getActivePolicy(SOME_AGENT_ID, SOME_OWNER_ID))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void shouldReturnPolicyById() {
        // given
        given(policyRepository.findByAgentIdAndPolicyId(SOME_AGENT_ID, SOME_POLICY_ID))
                .willReturn(Optional.of(SOME_ACTIVE_POLICY));

        // when
        var result = policyQueryHandler.getPolicy(SOME_AGENT_ID, SOME_POLICY_ID, SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_POLICY);
    }

    @Test
    void shouldThrowWhenPolicyNotFound() {
        // given
        given(policyRepository.findByAgentIdAndPolicyId(SOME_AGENT_ID, SOME_POLICY_ID))
                .willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> policyQueryHandler.getPolicy(SOME_AGENT_ID, SOME_POLICY_ID, SOME_OWNER_ID))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void shouldListPolicyHistory() {
        // given
        var pageable = PageRequest.of(0, 20);
        Page<Policy> page = new PageImpl<>(List.of(SOME_ACTIVE_POLICY, SOME_SUPERSEDED_POLICY), pageable, 2);
        given(policyRepository.findByAgentId(SOME_AGENT_ID, pageable)).willReturn(page);

        // when
        var result = policyQueryHandler.listPolicyHistory(SOME_AGENT_ID, SOME_OWNER_ID, pageable);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(page);
    }

    @Test
    void shouldThrowWhenAgentNotOwnedOnActiveQuery() {
        // given
        willThrow(new AgentOwnershipException(SOME_AGENT_ID, SOME_OWNER_ID))
                .given(agentAuthorization).verifyOwnership(SOME_AGENT_ID, SOME_OWNER_ID);

        // when / then
        assertThatThrownBy(() -> policyQueryHandler.getActivePolicy(SOME_AGENT_ID, SOME_OWNER_ID))
                .isInstanceOf(AgentOwnershipException.class);
    }

    @Test
    void shouldVerifyOwnershipBeforeReadingRepository() {
        // given
        var pageable = (Pageable) PageRequest.of(0, 20);
        willThrow(new AgentOwnershipException(SOME_AGENT_ID, SOME_OWNER_ID))
                .given(agentAuthorization).verifyOwnership(SOME_AGENT_ID, SOME_OWNER_ID);

        // when / then
        assertThatThrownBy(() -> policyQueryHandler.listPolicyHistory(SOME_AGENT_ID, SOME_OWNER_ID, pageable))
                .isInstanceOf(AgentOwnershipException.class);
        then(policyRepository).shouldHaveNoInteractions();
    }
}
