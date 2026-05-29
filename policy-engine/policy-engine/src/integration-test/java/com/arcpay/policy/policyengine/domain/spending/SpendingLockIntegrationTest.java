package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SpendingLockIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SpendingLockService spendingLockService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldSerializeConcurrentEvaluations() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        var transactionTemplate = new TransactionTemplate(transactionManager);
        var executor = Executors.newFixedThreadPool(2);
        var firstHoldingLock = new CountDownLatch(1);
        var firstReleased = new AtomicLong();
        var secondAcquired = new AtomicLong();
        var firstHoldMillis = 500L;

        // when
        var first = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            spendingLockService.acquireLock(agentId);
            firstHoldingLock.countDown();
            sleep(firstHoldMillis);
            firstReleased.set(System.nanoTime());
        }));

        firstHoldingLock.await(5, TimeUnit.SECONDS);

        var second = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            spendingLockService.acquireLock(agentId);
            secondAcquired.set(System.nanoTime());
        }));

        first.get(15, TimeUnit.SECONDS);
        second.get(15, TimeUnit.SECONDS);
        executor.shutdownNow();

        // then
        assertThat(secondAcquired.get()).isGreaterThanOrEqualTo(firstReleased.get());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
