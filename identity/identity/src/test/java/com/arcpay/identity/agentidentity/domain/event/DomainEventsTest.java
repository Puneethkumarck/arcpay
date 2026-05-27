package com.arcpay.identity.agentidentity.domain.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEventsTest {

    private static final UUID SOME_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    private static final Instant SOME_INSTANT = Instant.parse("2026-06-01T10:00:00Z");

    @ParameterizedTest
    @MethodSource("eventTopics")
    void shouldHaveCorrectTopicConstant(String topic, String expectedTopic) {
        // then
        assertThat(topic).isEqualTo(expectedTopic);
    }

    static Stream<Arguments> eventTopics() {
        return Stream.of(
                Arguments.of(OwnerRegistered.TOPIC, "owner.registered"),
                Arguments.of(AgentRegistrationRequested.TOPIC, "agent.registration-requested"),
                Arguments.of(AgentWalletProvisioned.TOPIC, "agent.wallet-provisioned"),
                Arguments.of(AgentOnChainRegistered.TOPIC, "agent.on-chain-registered"),
                Arguments.of(AgentActivated.TOPIC, "agent.activated"),
                Arguments.of(AgentMetadataUpdated.TOPIC, "agent.metadata-updated"),
                Arguments.of(AgentPolicyUpdated.TOPIC, "agent.policy-updated"),
                Arguments.of(AgentDeactivated.TOPIC, "agent.deactivated"),
                Arguments.of(AgentReactivated.TOPIC, "agent.reactivated"),
                Arguments.of(AgentProvisioningFailed.TOPIC, "agent.provisioning-failed")
        );
    }

    @Test
    void shouldConstructOwnerRegisteredWithRequiredFields() {
        // given / when
        var event = new OwnerRegistered(SOME_ID, "alice@example.com", "0xabc123", SOME_INSTANT);

        // then
        assertThat(event.ownerId()).isEqualTo(SOME_ID);
        assertThat(event.email()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldRejectNullOwnerIdInOwnerRegistered() {
        // given / when / then
        assertThatThrownBy(() -> new OwnerRegistered(null, "a@b.com", "0xabc", SOME_INSTANT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldConstructAgentRegistrationRequestedWithRequiredFields() {
        // given / when
        var event = new AgentRegistrationRequested(SOME_ID, SOME_ID, "agent-1", "trading", "0xhash", SOME_INSTANT);

        // then
        assertThat(event.agentId()).isEqualTo(SOME_ID);
        assertThat(event.name()).isEqualTo("agent-1");
    }

    @Test
    void shouldConstructAgentWalletProvisionedWithRequiredFields() {
        // given / when
        var event = new AgentWalletProvisioned(SOME_ID, "wallet-123", "0xabc", SOME_INSTANT);

        // then
        assertThat(event.walletId()).isEqualTo("wallet-123");
    }

    @Test
    void shouldConstructAgentOnChainRegisteredWithRequiredFields() {
        // given / when
        var event = new AgentOnChainRegistered(SOME_ID, "0xtxhash", 42L, SOME_INSTANT);

        // then
        assertThat(event.txHash()).isEqualTo("0xtxhash");
        assertThat(event.blockNumber()).isEqualTo(42L);
    }

    @Test
    void shouldConstructAgentProvisioningFailedWithRequiredFields() {
        // given / when
        var event = new AgentProvisioningFailed(SOME_ID, "WALLET_CREATION", "Circle API error", SOME_INSTANT);

        // then
        assertThat(event.failedStep()).isEqualTo("WALLET_CREATION");
        assertThat(event.reason()).isEqualTo("Circle API error");
    }

    @Test
    void shouldConstructAgentActivated() {
        // given / when
        var event = new AgentActivated(SOME_ID, SOME_INSTANT);

        // then
        assertThat(event.agentId()).isEqualTo(SOME_ID);
    }

    @Test
    void shouldConstructAgentDeactivated() {
        // given / when
        var event = new AgentDeactivated(SOME_ID, SOME_INSTANT);

        // then
        assertThat(event.agentId()).isEqualTo(SOME_ID);
    }

    @Test
    void shouldConstructAgentReactivated() {
        // given / when
        var event = new AgentReactivated(SOME_ID, SOME_INSTANT);

        // then
        assertThat(event.agentId()).isEqualTo(SOME_ID);
    }

    @Test
    void shouldConstructAgentMetadataUpdated() {
        // given / when
        var event = new AgentMetadataUpdated(SOME_ID, "new-name", "new-purpose", "0xhash", SOME_INSTANT);

        // then
        assertThat(event.name()).isEqualTo("new-name");
        assertThat(event.metadataHash()).isEqualTo("0xhash");
    }

    @Test
    void shouldConstructAgentPolicyUpdated() {
        // given / when
        var event = new AgentPolicyUpdated(SOME_ID, "0xpolicyhash", SOME_INSTANT);

        // then
        assertThat(event.policyHash()).isEqualTo("0xpolicyhash");
    }
}
