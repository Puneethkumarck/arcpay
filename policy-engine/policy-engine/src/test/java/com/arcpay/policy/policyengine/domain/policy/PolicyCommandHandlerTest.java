package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_AGENT;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY_WITH_COMPUTED_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_OWNED_BY_OTHER;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_COMPUTED_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_PRINCIPAL;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_RULES;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUSPENDED_AGENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PolicyCommandHandlerTest {

    @Mock
    private AgentServiceClient agentServiceClient;

    @Mock
    private PolicyValidator policyValidator;

    @Mock
    private PolicyCreationService policyCreationService;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private SpendingLockRepository spendingLockRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PolicyCommandHandler policyCommandHandler;

    private static Policy newActivePolicy(int version) {
        return Policy.builder()
                .policyId(SOME_AGENT_ID)
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .version(version)
                .rules(SOME_RULES)
                .policyHash(SOME_COMPUTED_HASH)
                .status(PolicyStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldCreateFirstPolicyForAgent() {
        // given
        var created = newActivePolicy(1);
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyRepository.findMaxVersionByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyCreationService.createPolicy(SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 1))
                .willReturn(created);
        given(policyRepository.save(eqIgnoringTimestamps(created))).willReturn(created);

        // when
        var result = policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(created);
        then(policyValidator).should().validate(SOME_RULES);
        then(spendingLockRepository).should().createIfNotExists(SOME_AGENT_ID);
        then(spendingLockRepository).should().acquireLock(SOME_AGENT_ID);
        then(policyRepository).should().save(eqIgnoringTimestamps(created));
    }

    @Test
    void shouldUpdatePolicyAndSupersedeOldVersion() {
        // given
        var existing = SOME_ACTIVE_POLICY_WITH_COMPUTED_HASH.toBuilder().version(1).build();
        var differentRulesHash = "0xdifferent";
        var existingDifferent = existing.toBuilder().policyHash(differentRulesHash).build();
        var created = newActivePolicy(2);
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(existingDifferent));
        given(policyRepository.findMaxVersionByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(1));
        given(policyCreationService.createPolicy(SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 2))
                .willReturn(created);
        given(policyRepository.save(eqIgnoringTimestamps(existingDifferent.supersede()))).willReturn(existingDifferent.supersede());
        given(policyRepository.save(eqIgnoringTimestamps(created))).willReturn(created);

        // when
        var result = policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES);

        // then
        assertThat(result.version()).isEqualTo(2);
        then(policyRepository).should().save(eqIgnoringTimestamps(existingDifferent.supersede()));
        then(policyRepository).should().save(eqIgnoringTimestamps(created));
    }

    @Test
    void shouldReturnExistingPolicyWhenSameHash() {
        // given
        var existing = SOME_ACTIVE_POLICY_WITH_COMPUTED_HASH;
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.of(existing));

        // when
        var result = policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existing);
        then(policyRepository).should(never()).save(eqIgnoringTimestamps(existing));
        then(policyRepository).should(never()).findMaxVersionByAgentId(SOME_AGENT_ID);
        then(agentServiceClient).should(never()).updatePolicy(SOME_AGENT_ID, SOME_COMPUTED_HASH);
        then(eventPublisher).should(never()).publish(eqIgnoring(
                new PolicyCreated(existing.policyId(), SOME_AGENT_ID, SOME_OWNER_ID, 1, SOME_COMPUTED_HASH, Instant.now()),
                "createdAt"));
    }

    @Test
    void shouldRejectWhenAgentNotFound() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES))
                .isInstanceOf(AgentNotFoundException.class);
    }

    @Test
    void shouldRejectWhenAgentNotOwnedByPrincipal() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_AGENT_OWNED_BY_OTHER));

        // when / then
        assertThatThrownBy(() -> policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES))
                .isInstanceOf(AgentOwnershipException.class);
    }

    @Test
    void shouldRejectWhenAgentNotActive() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_SUSPENDED_AGENT));

        // when / then
        assertThatThrownBy(() -> policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES))
                .isInstanceOf(AgentNotActiveException.class);
    }

    @Test
    void shouldRejectInvalidRules() {
        // given
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        org.mockito.BDDMockito.willThrow(new InvalidPolicyException("bad rules"))
                .given(policyValidator).validate(SOME_RULES);

        // when / then
        assertThatThrownBy(() -> policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES))
                .isInstanceOf(InvalidPolicyException.class);
        then(policyRepository).should(never()).save(eqIgnoringTimestamps(newActivePolicy(1)));
    }

    @Test
    void shouldSyncPolicyHashToIdentityService() {
        // given
        var created = newActivePolicy(1);
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyRepository.findMaxVersionByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyCreationService.createPolicy(SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 1))
                .willReturn(created);
        given(policyRepository.save(eqIgnoringTimestamps(created))).willReturn(created);

        // when
        policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES);

        // then
        then(agentServiceClient).should().updatePolicy(SOME_AGENT_ID, SOME_COMPUTED_HASH);
    }

    @Test
    void shouldPublishPolicyCreatedEvent() {
        // given
        var created = newActivePolicy(1);
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(SOME_ACTIVE_AGENT));
        given(policyRepository.findActiveByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyRepository.findMaxVersionByAgentId(SOME_AGENT_ID)).willReturn(Optional.empty());
        given(policyCreationService.createPolicy(SOME_AGENT_ID, SOME_OWNER_ID, SOME_RULES, SOME_COMPUTED_HASH, 1))
                .willReturn(created);
        given(policyRepository.save(eqIgnoringTimestamps(created))).willReturn(created);

        // when
        policyCommandHandler.createOrUpdatePolicy(SOME_AGENT_ID, SOME_OWNER_PRINCIPAL, SOME_RULES);

        // then
        then(eventPublisher).should().publish(eqIgnoring(
                new PolicyCreated(
                        created.policyId(), SOME_AGENT_ID, SOME_OWNER_ID, 1, SOME_COMPUTED_HASH, Instant.now()),
                "createdAt"));
    }
}
