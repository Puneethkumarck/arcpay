package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.OTHER_OWNER_ID;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AgentJpaRepositoryIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private AgentJpaRepository agentJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedOwners() {
        jdbcTemplate.update("""
                INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                ON CONFLICT DO NOTHING""",
                SOME_OWNER_ID, "alice@example.com",
                "0x1234567890abcdef1234567890abcdef12345678", "a".repeat(64));
        jdbcTemplate.update("""
                INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                ON CONFLICT DO NOTHING""",
                OTHER_OWNER_ID, "bob@example.com",
                "0xfedcba9876543210fedcba9876543210fedcba98", "b".repeat(64));
    }

    @Test
    void shouldFindByIdForUpdateReturnEntityWhenInTransaction() {
        // given
        var entity = newAgentEntity(SOME_OWNER_ID, "lock-agent", AgentStatus.ACTIVE);
        agentJpaRepository.save(entity);

        // when
        var result = agentJpaRepository.findByIdForUpdate(entity.getAgentId()).orElseThrow();

        // then
        assertThat(result).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(entity);
    }

    @Test
    void shouldReportTotalCountFilteredByOwnerAndStatus() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-1", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-2", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-3", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "suspended-1", AgentStatus.SUSPENDED));
        agentJpaRepository.save(newAgentEntity(OTHER_OWNER_ID, "other-active", AgentStatus.ACTIVE));

        // when
        var page = agentJpaRepository.findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void shouldReturnOnlyRequestedStatusWhenPaginating() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-1", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-2", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "suspended-1", AgentStatus.SUSPENDED));

        // when
        var page = agentJpaRepository.findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent())
                .extracting(AgentEntity::getStatus)
                .containsOnly(AgentStatus.ACTIVE);
    }

    @Test
    void shouldPaginateSecondPageWhenResultsExceedPageSize() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-1", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-2", AgentStatus.ACTIVE));
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "active-3", AgentStatus.ACTIVE));

        // when
        var secondPage = agentJpaRepository.findByOwnerIdAndStatus(SOME_OWNER_ID, AgentStatus.ACTIVE, PageRequest.of(1, 2));

        // then
        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    void shouldDetectNameExistsCaseInsensitivePerOwner() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "Shopping-Agent", AgentStatus.ACTIVE));

        // when
        var lowerExists = agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(SOME_OWNER_ID, "shopping-agent");
        var upperExists = agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(SOME_OWNER_ID, "SHOPPING-AGENT");
        var otherOwnerSeesNothing = agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(OTHER_OWNER_ID, "shopping-agent");

        // then
        assertThat(lowerExists).isTrue();
        assertThat(upperExists).isTrue();
        assertThat(otherOwnerSeesNothing).isFalse();
    }

    @Test
    void shouldAllowSameNameForDifferentOwners() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "shared-name", AgentStatus.ACTIVE));

        // when
        agentJpaRepository.save(newAgentEntity(OTHER_OWNER_ID, "shared-name", AgentStatus.ACTIVE));

        // then
        var someOwnerHas = agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(SOME_OWNER_ID, "shared-name");
        var otherOwnerHas = agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(OTHER_OWNER_ID, "shared-name");
        assertThat(someOwnerHas).isTrue();
        assertThat(otherOwnerHas).isTrue();
    }

    @Test
    void shouldRejectDuplicateNameForSameOwner() {
        // given
        agentJpaRepository.save(newAgentEntity(SOME_OWNER_ID, "unique-name", AgentStatus.ACTIVE));

        // when / then
        assertThatThrownBy(() ->
                agentJpaRepository.saveAndFlush(newAgentEntity(SOME_OWNER_ID, "UNIQUE-NAME", AgentStatus.ACTIVE))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldExcludeAgentByIdWhenCheckingNameExists() {
        // given
        var existing = newAgentEntity(SOME_OWNER_ID, "rename-test", AgentStatus.ACTIVE);
        agentJpaRepository.save(existing);

        // when
        var conflictsWithOther = agentJpaRepository
                .existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(SOME_OWNER_ID, "rename-test", UUID.randomUUID());
        var conflictsWithSelf = agentJpaRepository
                .existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(SOME_OWNER_ID, "rename-test", existing.getAgentId());

        // then
        assertThat(conflictsWithOther).isTrue();
        assertThat(conflictsWithSelf).isFalse();
    }

    private AgentEntity newAgentEntity(UUID ownerId, String name, AgentStatus status) {
        var now = Instant.now();
        return AgentEntity.builder()
                .agentId(UUID.randomUUID())
                .ownerId(ownerId)
                .name(name)
                .purpose("test purpose for " + name)
                .status(status)
                .metadataHash("0x" + "a".repeat(64))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
