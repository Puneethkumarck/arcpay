package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCreationServiceTest {

    private final AgentCreationService agentCreationService = new AgentCreationService();

    private static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

    @Test
    void shouldCreateAgentWithUuidV7() {
        // given / when
        var agent = agentCreationService.createAgent(SOME_OWNER_ID, "my-agent", "trading", null);

        // then
        assertThat(agent.agentId()).isNotNull();
        assertThat(agent.agentId().version()).isEqualTo(7);
    }

    @Test
    void shouldSetStatusToProvisioning() {
        // given / when
        var agent = agentCreationService.createAgent(SOME_OWNER_ID, "my-agent", "trading", null);

        // then
        assertThat(agent.status()).isEqualTo(AgentStatus.PROVISIONING);
    }

    @Test
    void shouldComputeMetadataHash() {
        // given
        var expectedHash = MetadataHashUtil.computeMetadataHash("my-agent", "trading");

        // when
        var agent = agentCreationService.createAgent(SOME_OWNER_ID, "my-agent", "trading", null);

        // then
        assertThat(agent.metadataHash()).isEqualTo(expectedHash);
    }

    @Test
    void shouldSetOwnerIdAndName() {
        // given / when
        var agent = agentCreationService.createAgent(SOME_OWNER_ID, "my-agent", "trading", "0x" + "a".repeat(64));

        // then
        assertThat(agent.ownerId()).isEqualTo(SOME_OWNER_ID);
        assertThat(agent.name()).isEqualTo("my-agent");
        assertThat(agent.purpose()).isEqualTo("trading");
        assertThat(agent.policyHash()).isEqualTo("0x" + "a".repeat(64));
    }

    @Test
    void shouldSetTimestamps() {
        // given / when
        var agent = agentCreationService.createAgent(SOME_OWNER_ID, "my-agent", "trading", null);

        // then
        assertThat(agent.createdAt()).isEqualTo(agent.updatedAt());
    }
}
