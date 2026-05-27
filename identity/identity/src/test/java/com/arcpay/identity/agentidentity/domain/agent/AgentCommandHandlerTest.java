package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.event.AgentDeactivated;
import com.arcpay.identity.agentidentity.domain.event.AgentMetadataUpdated;
import com.arcpay.identity.agentidentity.domain.event.AgentPolicyUpdated;
import com.arcpay.identity.agentidentity.domain.event.AgentReactivated;
import com.arcpay.identity.agentidentity.domain.event.AgentRegistrationRequested;
import com.arcpay.identity.agentidentity.domain.exception.AgentNameDuplicateException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotInExpectedStateException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidAgentNameException;
import com.arcpay.identity.agentidentity.domain.exception.InvalidPolicyHashException;
import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
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
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_SUSPENDED;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class AgentCommandHandlerTest {

    @Mock
    private AgentValidator agentValidator;

    @Mock
    private AgentCreationService agentCreationService;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AgentCommandHandler agentCommandHandler;

    @Test
    void shouldRegisterAgentInProvisioningState() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        given(agentCreationService.createAgent(SOME_OWNER_ID, agent.name(), agent.purpose(), agent.policyHash()))
                .willReturn(agent);
        given(agentRepository.save(eqIgnoringTimestamps(agent))).willReturn(agent);

        // when
        var result = agentCommandHandler.registerAgent(SOME_OWNER_ID, agent.name(), agent.purpose(), agent.policyHash());

        // then
        assertThat(result.status()).isEqualTo(AgentStatus.PROVISIONING);
        then(agentValidator).should().validateRegistration(SOME_OWNER_ID, agent.name(), agent.purpose(), agent.policyHash());
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentRegistrationRequested(agent.agentId(), SOME_OWNER_ID, agent.name(), agent.purpose(), agent.metadataHash(), Instant.now()),
                "requestedAt"));
    }

    @Test
    void shouldThrowWhenAgentNameDuplicate() {
        // given
        willThrow(new AgentNameDuplicateException("my-agent", SOME_OWNER_ID))
                .given(agentValidator).validateRegistration(SOME_OWNER_ID, "my-agent", "purpose", null);

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.registerAgent(SOME_OWNER_ID, "my-agent", "purpose", null))
                .isInstanceOf(AgentNameDuplicateException.class);
    }

    @Test
    void shouldThrowWhenAgentNameInvalid() {
        // given
        willThrow(new InvalidAgentNameException("ab"))
                .given(agentValidator).validateRegistration(SOME_OWNER_ID, "ab", "purpose", null);

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.registerAgent(SOME_OWNER_ID, "ab", "purpose", null))
                .isInstanceOf(InvalidAgentNameException.class);
    }

    @Test
    void shouldUpdateMetadataAndRecomputeHash() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var newHash = MetadataHashUtil.computeMetadataHash("new-name", agent.purpose());
        var updatedAgent = agent.updateMetadata("new-name", agent.purpose(), newHash);
        given(agentRepository.save(eqIgnoringTimestamps(updatedAgent))).willReturn(updatedAgent);

        // when
        var result = agentCommandHandler.updateMetadata(agent.agentId(), SOME_OWNER_ID, "new-name", null);

        // then
        assertThat(result.name()).isEqualTo("new-name");
        assertThat(result.metadataHash()).isEqualTo(newHash);
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentMetadataUpdated(agent.agentId(), "new-name", agent.purpose(), newHash, Instant.now()),
                "updatedAt"));
    }

    @Test
    void shouldThrowWhenUpdatingProvisioningAgent() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.updateMetadata(agent.agentId(), SOME_OWNER_ID, "new-name", null))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }

    @Test
    void shouldThrowWhenUpdateCausesNameConflict() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        willThrow(new AgentNameDuplicateException("new-name", SOME_OWNER_ID))
                .given(agentValidator).validateUpdate(SOME_OWNER_ID, agent.agentId(), "new-name", null);

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.updateMetadata(agent.agentId(), SOME_OWNER_ID, "new-name", null))
                .isInstanceOf(AgentNameDuplicateException.class);
    }

    @Test
    void shouldDeactivateActiveAgent() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var deactivated = agent.deactivate();
        given(agentRepository.save(eqIgnoringTimestamps(deactivated))).willReturn(deactivated);

        // when
        var result = agentCommandHandler.deactivate(agent.agentId(), SOME_OWNER_ID);

        // then
        assertThat(result.status()).isEqualTo(AgentStatus.SUSPENDED);
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentDeactivated(agent.agentId(), Instant.now()),
                "deactivatedAt"));
    }

    @Test
    void shouldThrowWhenDeactivatingNonActiveAgent() {
        // given
        var agent = SOME_AGENT_PROVISIONING;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.deactivate(agent.agentId(), SOME_OWNER_ID))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }

    @Test
    void shouldReactivateSuspendedAgent() {
        // given
        var agent = SOME_AGENT_SUSPENDED;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var reactivated = agent.reactivate();
        given(agentRepository.save(eqIgnoringTimestamps(reactivated))).willReturn(reactivated);

        // when
        var result = agentCommandHandler.reactivate(agent.agentId(), SOME_OWNER_ID);

        // then
        assertThat(result.status()).isEqualTo(AgentStatus.ACTIVE);
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentReactivated(agent.agentId(), Instant.now()),
                "reactivatedAt"));
    }

    @Test
    void shouldThrowWhenReactivatingNonSuspendedAgent() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.reactivate(agent.agentId(), SOME_OWNER_ID))
                .isInstanceOf(AgentNotInExpectedStateException.class);
    }

    @Test
    void shouldUpdatePolicyHash() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var newPolicyHash = "0x" + "b".repeat(64);
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));
        var updatedAgent = agent.toBuilder().policyHash(newPolicyHash).updatedAt(Instant.now()).build();
        given(agentRepository.save(eqIgnoringTimestamps(updatedAgent))).willReturn(updatedAgent);

        // when
        var result = agentCommandHandler.updatePolicy(agent.agentId(), SOME_OWNER_ID, newPolicyHash);

        // then
        assertThat(result.policyHash()).isEqualTo(newPolicyHash);
        then(eventPublisher).should().publish(eqIgnoring(
                new AgentPolicyUpdated(agent.agentId(), newPolicyHash, Instant.now()),
                "updatedAt"));
    }

    @Test
    void shouldThrowWhenPolicyHashInvalid() {
        // given
        willThrow(new InvalidPolicyHashException("0xbad"))
                .given(agentValidator).validatePolicyUpdate("0xbad");

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.updatePolicy(
                SOME_AGENT_ACTIVE.agentId(), SOME_OWNER_ID, "0xbad"))
                .isInstanceOf(InvalidPolicyHashException.class);
    }

    @Test
    void shouldThrowWhenAgentNotFound() {
        // given
        var unknownId = UUID.randomUUID();
        given(agentRepository.findByIdForUpdate(unknownId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.deactivate(unknownId, SOME_OWNER_ID))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldThrowForbiddenWhenOwnerMismatch() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var wrongOwnerId = UUID.randomUUID();
        given(agentRepository.findByIdForUpdate(agent.agentId())).willReturn(Optional.of(agent));

        // when / then
        assertThatThrownBy(() -> agentCommandHandler.deactivate(agent.agentId(), wrongOwnerId))
                .isInstanceOf(ForbiddenException.class);
    }
}
