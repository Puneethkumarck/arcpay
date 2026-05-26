package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.ProvisioningStatus;
import com.arcpay.identity.agentidentity.domain.model.StepStatus;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_FAILED;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_SUSPENDED;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_WALLET_READY;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AgentQueryHandlerTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentQueryHandler agentQueryHandler;

    @Test
    void shouldReturnAgentForCorrectOwner() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findById(agent.agentId())).willReturn(Optional.of(agent));

        // when
        var result = agentQueryHandler.getAgent(agent.agentId(), SOME_OWNER_ID);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldThrowForbiddenForWrongOwner() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var wrongOwnerId = UUID.randomUUID();
        given(agentRepository.findById(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentQueryHandler.getAgent(agent.agentId(), wrongOwnerId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldThrowNotFoundForUnknownAgent() {
        // given
        var unknownId = UUID.randomUUID();
        given(agentRepository.findById(unknownId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> agentQueryHandler.getAgent(unknownId, SOME_OWNER_ID))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldDelegateListToRepositoryWithStatus() {
        // given
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(SOME_AGENT_ACTIVE));
        given(agentRepository.findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, pageable))
                .willReturn(page);

        // when
        var result = agentQueryHandler.listAgents(SOME_OWNER_ID, AgentStatus.ACTIVE, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        then(agentRepository).should().findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, pageable);
    }

    @Test
    void shouldDelegateListToRepositoryWithoutStatus() {
        // given
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(SOME_AGENT_ACTIVE, SOME_AGENT_SUSPENDED));
        given(agentRepository.findByOwnerId(SOME_OWNER_ID, pageable)).willReturn(page);

        // when
        var result = agentQueryHandler.listAgents(SOME_OWNER_ID, null, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        then(agentRepository).should().findByOwnerId(SOME_OWNER_ID, pageable);
    }

    @ParameterizedTest
    @MethodSource("provisioningStatusMatrix")
    void shouldDeriveProvisioningStatusForEachAgentState(
            Agent agent, StepStatus expectedWallet, StepStatus expectedOnChain) {
        // given
        given(agentRepository.findById(agent.agentId())).willReturn(Optional.of(agent));

        // when
        var result = agentQueryHandler.getProvisioningStatus(agent.agentId(), SOME_OWNER_ID);

        // then
        var expected = new ProvisioningStatus(agent.agentId(), agent.status(), expectedWallet, expectedOnChain);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    static Stream<Arguments> provisioningStatusMatrix() {
        return Stream.of(
                Arguments.of(SOME_AGENT_PROVISIONING, StepStatus.IN_PROGRESS, StepStatus.PENDING),
                Arguments.of(SOME_AGENT_WALLET_READY, StepStatus.COMPLETED, StepStatus.IN_PROGRESS),
                Arguments.of(SOME_AGENT_ACTIVE, StepStatus.COMPLETED, StepStatus.COMPLETED),
                Arguments.of(SOME_AGENT_SUSPENDED, StepStatus.COMPLETED, StepStatus.COMPLETED)
        );
    }

    @Test
    void shouldDeriveWalletCreationFailedWhenWalletIdNull() {
        // given
        var failedAgent = SOME_AGENT_FAILED;
        given(agentRepository.findById(failedAgent.agentId())).willReturn(Optional.of(failedAgent));

        // when
        var result = agentQueryHandler.getProvisioningStatus(failedAgent.agentId(), SOME_OWNER_ID);

        // then
        assertThat(result.walletCreation()).isEqualTo(StepStatus.FAILED);
        assertThat(result.onChainRegistration()).isEqualTo(StepStatus.PENDING);
    }

    @Test
    void shouldDeriveOnChainFailedWhenWalletIdPresent() {
        // given
        var failedOnChain = SOME_AGENT_FAILED.toBuilder()
                .walletId("wallet-123")
                .walletAddress("0xabc")
                .build();
        given(agentRepository.findById(failedOnChain.agentId())).willReturn(Optional.of(failedOnChain));

        // when
        var result = agentQueryHandler.getProvisioningStatus(failedOnChain.agentId(), SOME_OWNER_ID);

        // then
        assertThat(result.walletCreation()).isEqualTo(StepStatus.COMPLETED);
        assertThat(result.onChainRegistration()).isEqualTo(StepStatus.FAILED);
    }
}
