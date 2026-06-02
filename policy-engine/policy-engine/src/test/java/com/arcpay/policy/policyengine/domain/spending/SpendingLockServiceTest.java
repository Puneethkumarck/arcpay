package com.arcpay.policy.policyengine.domain.spending;

import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpendingLockServiceTest {

    @Mock
    private SpendingLockRepository spendingLockRepository;

    @InjectMocks
    private SpendingLockService spendingLockService;

    @Test
    void shouldCreateAndAcquireLock() {
        // given / when
        spendingLockService.acquireLock(SOME_AGENT_ID);

        // then
        InOrder inOrder = inOrder(spendingLockRepository);
        then(spendingLockRepository).should(inOrder).createIfNotExists(SOME_AGENT_ID);
        then(spendingLockRepository).should(inOrder).acquireLock(SOME_AGENT_ID);
    }
}
