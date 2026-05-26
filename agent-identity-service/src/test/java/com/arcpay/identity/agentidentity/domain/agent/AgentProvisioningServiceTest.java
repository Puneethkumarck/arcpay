package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.event.AgentActivated;
import com.arcpay.identity.agentidentity.domain.event.AgentOnChainRegistered;
import com.arcpay.identity.agentidentity.domain.event.AgentProvisioningFailed;
import com.arcpay.identity.agentidentity.domain.event.AgentWalletProvisioned;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotInExpectedStateException;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_WALLET_READY;
import static com.arcpay.identity.agentidentity.test.TestUtils.eqIgnoring;
import static com.arcpay.identity.agentidentity.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AgentProvisioningServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AgentProvisioningService agentProvisioningService;

    @Test
    void shouldCompleteWalletCreationAndTransitionToWalletReady() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        var walletId = "wallet-123";
        var walletAddress = "0xabc123";
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var updatedAgent = agent.withWallet(walletId, walletAddress);

        // when
        agentProvisioningService.completeWalletCreation(agent.agentId(), walletId, walletAddress);

        // then
        then(agentRepository).should().save(eqIgnoringTimestamps(updatedAgent));
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentWalletProvisioned(agent.agentId(), walletId, walletAddress, Instant.now()),
                "provisionedAt"));
    }

    @Test
    void shouldThrowWhenCompletingWalletOnNonProvisioningAgent() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentProvisioningService.completeWalletCreation(
                agent.agentId(), "wallet-id", "0xabc"))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }

    @Test
    void shouldThrowWhenAgentNotFoundForWalletCompletion() {
        // given
        var unknownId = UUID.randomUUID();
        given(agentRepository.findByIdForUpdate(unknownId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> agentProvisioningService.completeWalletCreation(
                unknownId, "wallet-id", "0xabc"))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldCompleteOnChainAndPublishTwoEvents() {
        // given
        var agent = SOME_AGENT_WALLET_READY;
        var txHash = "0xdeadbeef";
        var blockNumber = 42L;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var updatedAgent = agent.withOnChainRegistration(txHash);

        // when
        agentProvisioningService.completeOnChainRegistration(agent.agentId(), txHash, blockNumber);

        // then
        then(agentRepository).should().save(eqIgnoringTimestamps(updatedAgent));
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentOnChainRegistered(agent.agentId(), txHash, blockNumber, Instant.now()),
                "registeredAt"));
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentActivated(agent.agentId(), Instant.now()),
                "activatedAt"));
    }

    @Test
    void shouldThrowWhenCompletingOnChainOnNonWalletReadyAgent() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentProvisioningService.completeOnChainRegistration(
                agent.agentId(), "0xtxhash", 1L))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }

    @Test
    void shouldFailProvisioningFromProvisioningState() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var failedAgent = agent.withFailure("Circle API error");

        // when
        agentProvisioningService.failProvisioning(agent.agentId(), "WALLET_CREATION", "Circle API error");

        // then
        then(agentRepository).should().save(eqIgnoringTimestamps(failedAgent));
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentProvisioningFailed(agent.agentId(), "WALLET_CREATION", "Circle API error", Instant.now()),
                "failedAt"));
    }

    @Test
    void shouldFailProvisioningFromWalletReadyState() {
        // given
        var agent = SOME_AGENT_WALLET_READY;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when
        agentProvisioningService.failProvisioning(agent.agentId(), "ON_CHAIN_REGISTRATION", "Gas estimation failed");

        // then
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentProvisioningFailed(agent.agentId(), "ON_CHAIN_REGISTRATION", "Gas estimation failed", Instant.now()),
                "failedAt"));
    }

    @Test
    void shouldThrowWhenFailingActiveAgent() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentProvisioningService.failProvisioning(
                agent.agentId(), "WALLET_CREATION", "some error"))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }
}
