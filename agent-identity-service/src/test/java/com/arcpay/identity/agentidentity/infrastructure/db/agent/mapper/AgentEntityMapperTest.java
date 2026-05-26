package com.arcpay.identity.agentidentity.infrastructure.db.agent.mapper;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.infrastructure.db.agent.AgentEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_FAILED;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static org.assertj.core.api.Assertions.assertThat;

class AgentEntityMapperTest {

    private final AgentEntityMapper mapper = Mappers.getMapper(AgentEntityMapper.class);

    @Test
    void shouldMapDomainToEntityWithAllFieldsPopulated() {
        // given
        var agent = SOME_AGENT_ACTIVE;

        // when
        var result = mapper.mapToEntity(agent);

        // then
        var expected = AgentEntity.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(agent.status())
                .walletId(agent.walletId())
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .failureReason(agent.failureReason())
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapDomainToEntityWithNullWalletAndOnChainFields() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when
        var result = mapper.mapToEntity(agent);

        // then
        var expected = AgentEntity.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(AgentStatus.PROVISIONING)
                .walletId(null)
                .walletAddress(null)
                .onChainTxHash(null)
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .failureReason(null)
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldMapEntityToDomainWithAllFieldsPopulated() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var entity = AgentEntity.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(agent.status())
                .walletId(agent.walletId())
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .failureReason(agent.failureReason())
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldMapEntityToDomainWithFailureReasonAndNullWalletFields() {
        // given
        var agent = SOME_AGENT_FAILED;
        var entity = AgentEntity.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(AgentStatus.FAILED)
                .walletId(null)
                .walletAddress(null)
                .onChainTxHash(null)
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .failureReason(agent.failureReason())
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();

        // when
        var result = mapper.mapToDomain(entity);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldRoundTripDomainToEntityToDomain() {
        // given
        var original = SOME_AGENT_ACTIVE;

        // when
        var result = mapper.mapToDomain(mapper.mapToEntity(original));

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void shouldReturnNullWhenMapToEntityWithNullInput() {
        // given / when
        var result = mapper.mapToEntity(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenMapToDomainWithNullInput() {
        // given / when
        var result = mapper.mapToDomain(null);

        // then
        assertThat(result).isNull();
    }
}
