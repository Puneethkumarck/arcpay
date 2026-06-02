package com.arcpay.policy.policyengine.infrastructure.db.spending;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SpendingLockRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SpendingLockRepository spendingLockRepository;

    @Test
    void shouldCreateLockAndAcquire() {
        // given
        var agentId = UUID.randomUUID();

        // when / then
        assertThatCode(() -> spendingLockRepository.acquireLock(agentId)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcquireExistingLock() {
        // given
        var agentId = UUID.randomUUID();
        spendingLockRepository.createIfNotExists(agentId);

        // when / then
        assertThatCode(() -> spendingLockRepository.acquireLock(agentId)).doesNotThrowAnyException();
    }
}
