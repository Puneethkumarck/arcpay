package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.fixtures.OwnerFixtures;
import com.arcpay.identity.agentidentity.infrastructure.db.owner.OwnerJpaRepository;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_FAILED;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_SUSPENDED;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;

class AgentRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private OwnerJpaRepository ownerJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM agents");
        jdbcTemplate.update("DELETE FROM owners");
        ownerJpaRepository.save(OwnerFixtures.SOME_OWNER_ENTITY.toBuilder().build());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM agents");
        jdbcTemplate.update("DELETE FROM owners");
    }

    @Test
    void shouldRoundTripProvisioningAgent() {
        // given
        var agent = SOME_AGENT_PROVISIONING;

        // when
        agentRepository.save(agent);
        var loaded = agentRepository.findById(agent.agentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldRoundTripActiveAgent() {
        // given
        var agent = SOME_AGENT_ACTIVE;

        // when
        agentRepository.save(agent);
        var loaded = agentRepository.findById(agent.agentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldRoundTripSuspendedAgent() {
        // given
        var agent = SOME_AGENT_SUSPENDED;

        // when
        agentRepository.save(agent);
        var loaded = agentRepository.findById(agent.agentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldRoundTripFailedAgent() {
        // given
        var agent = SOME_AGENT_FAILED;

        // when
        agentRepository.save(agent);
        var loaded = agentRepository.findById(agent.agentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldFindByIdForUpdateReturningDomainModel() {
        // given
        var agent = SOME_AGENT_ACTIVE;
        agentRepository.save(agent);

        // when
        var loaded = transactionTemplate.execute(status ->
                agentRepository.findByIdForUpdate(agent.agentId()).orElseThrow());

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(agent);
    }

    @Test
    void shouldFindByOwnerIdAndStatusPaginated() {
        // given
        agentRepository.save(SOME_AGENT_ACTIVE);
        agentRepository.save(SOME_AGENT_SUSPENDED);

        // when
        var activePage = agentRepository.findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(activePage.getTotalElements()).isEqualTo(1);
        assertThat(activePage.getContent().getFirst())
                .usingRecursiveComparison()
                .isEqualTo(SOME_AGENT_ACTIVE);
    }

    @Test
    void shouldFindByOwnerIdPaginatedAcrossAllStatuses() {
        // given
        agentRepository.save(SOME_AGENT_PROVISIONING);
        agentRepository.save(SOME_AGENT_ACTIVE);
        agentRepository.save(SOME_AGENT_SUSPENDED);
        agentRepository.save(SOME_AGENT_FAILED);

        // when
        var page = agentRepository.findByOwnerId(SOME_OWNER_ID, PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    void shouldReturnEmptyOptionalWhenAgentMissing() {
        // given
        var randomId = UUID.randomUUID();

        // when
        var result = agentRepository.findById(randomId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReportNameDoesNotExistWhenNoAgentsRegistered() {
        // given / when
        var exists = agentRepository.existsByOwnerIdAndNameIgnoreCase(SOME_OWNER_ID, "missing-name");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldUpdateAgentViaStateTransition() {
        // given
        agentRepository.save(SOME_AGENT_PROVISIONING);

        // when
        var transitioned = SOME_AGENT_PROVISIONING.withWallet("wallet-xyz", "0xabcabcabcabcabcabcabcabcabcabcabcabcabca");
        agentRepository.save(transitioned);
        var loaded = agentRepository.findById(SOME_AGENT_PROVISIONING.agentId()).orElseThrow();

        // then
        assertThat(loaded)
                .usingRecursiveComparison()
                .ignoringFields("updatedAt")
                .isEqualTo(transitioned);
    }
}
