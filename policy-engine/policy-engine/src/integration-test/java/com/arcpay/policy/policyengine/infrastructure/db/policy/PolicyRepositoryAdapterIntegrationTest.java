package com.arcpay.policy.policyengine.infrastructure.db.policy;

import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUPERSEDED_POLICY;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PolicyRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    void shouldSaveAndFindPolicy() {
        // given
        var policy = SOME_ACTIVE_POLICY;

        // when
        policyRepository.save(policy);
        var loaded = policyRepository.findById(policy.policyId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(policy);
    }

    @Test
    void shouldFindActiveByAgentId() {
        // given
        policyRepository.save(SOME_ACTIVE_POLICY);

        // when
        var result = policyRepository.findActiveByAgentId(SOME_AGENT_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_POLICY);
    }

    @Test
    void shouldFindMaxVersion() {
        // given
        policyRepository.save(SOME_ACTIVE_POLICY);
        var v2 = SOME_ACTIVE_POLICY.toBuilder()
                .policyId(UUID.randomUUID())
                .version(2)
                .status(PolicyStatus.ACTIVE)
                .build();
        policyRepository.save(v2);

        // when
        var maxVersion = policyRepository.findMaxVersionByAgentId(SOME_AGENT_ID);

        // then
        assertThat(maxVersion).isPresent().hasValue(2);
    }

    @Test
    void shouldReturnEmptyWhenNoPolicyExists() {
        // given
        var randomAgentId = UUID.randomUUID();

        // when
        var result = policyRepository.findActiveByAgentId(randomAgentId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindByAgentIdPaginated() {
        // given
        policyRepository.save(SOME_ACTIVE_POLICY);
        var v2 = SOME_ACTIVE_POLICY.toBuilder()
                .policyId(UUID.randomUUID())
                .version(2)
                .status(PolicyStatus.SUPERSEDED)
                .build();
        policyRepository.save(v2);

        // when
        var page = policyRepository.findByAgentId(SOME_AGENT_ID, PageRequest.of(0, 10));

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFindByAgentIdAndPolicyId() {
        // given
        policyRepository.save(SOME_ACTIVE_POLICY);

        // when
        var result = policyRepository.findByAgentIdAndPolicyId(SOME_AGENT_ID, SOME_ACTIVE_POLICY.policyId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).usingRecursiveComparison().isEqualTo(SOME_ACTIVE_POLICY);
    }

    @Test
    void shouldReturnEmptyMaxVersionWhenNoPolicy() {
        // given
        var randomAgentId = UUID.randomUUID();

        // when
        var maxVersion = policyRepository.findMaxVersionByAgentId(randomAgentId);

        // then
        assertThat(maxVersion).isEmpty();
    }
}
