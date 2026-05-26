package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningService;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService.WalletCreationResult;
import io.temporal.failure.ApplicationFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_WALLET_READY;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_TX_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AgentProvisioningActivitiesImplTest {

    @Mock
    private CircleWalletService circleWalletService;

    @Mock
    private BlockchainService blockchainService;

    @Mock
    private AgentProvisioningService agentProvisioningService;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentProvisioningActivitiesImpl activities;

    @Test
    void shouldCreateWalletAndCompleteWalletCreation() {
        // given
        var walletResult = new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS);
        given(circleWalletService.createWallet(SOME_AGENT_ID)).willReturn(walletResult);

        // when
        activities.createCircleWallet(SOME_AGENT_ID);

        // then
        then(agentProvisioningService).should().completeWalletCreation(
                SOME_AGENT_ID, SOME_WALLET_ID, SOME_WALLET_ADDRESS);
    }

    @Test
    void shouldRegisterOnChainAndCompleteRegistration() {
        // given
        var agent = SOME_AGENT_WALLET_READY;
        var registrationResult = new RegistrationResult(SOME_TX_HASH, 42L);
        given(agentRepository.findById(agent.agentId())).willReturn(Optional.of(agent));
        given(blockchainService.registerAgent(agent.agentId(), agent.ownerId(), agent.metadataHash()))
                .willReturn(registrationResult);

        // when
        activities.registerOnChain(agent.agentId());

        // then
        then(agentProvisioningService).should().completeOnChainRegistration(
                agent.agentId(), SOME_TX_HASH, 42L);
    }

    @Test
    void shouldThrowNonRetryableFailureWhenAgentNotFound() {
        // given
        given(agentRepository.findById(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> activities.registerOnChain(SOME_AGENT_ID))
                .isInstanceOfSatisfying(ApplicationFailure.class, af -> {
                    assertThat(af.isNonRetryable()).isTrue();
                    assertThat(af.getMessage()).contains("Agent not found");
                });
    }

    @Test
    void shouldDelegateFailProvisioningToService() {
        // when
        activities.failProvisioning(SOME_AGENT_ID, "WALLET_CREATION", "Circle API error");

        // then
        then(agentProvisioningService).should().failProvisioning(
                SOME_AGENT_ID, "WALLET_CREATION", "Circle API error");
    }
}
