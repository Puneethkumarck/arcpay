package com.arcpay.policy.policyengine.infrastructure.client.identity;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResilienceConfigTest {

    private final ResilienceConfig config = new ResilienceConfig();

    @Test
    void shouldOpenCircuitAfterConfiguredFailures() {
        // given
        CircuitBreaker circuitBreaker = config.identityCircuitBreaker();

        // when — record 10 failures (minimumNumberOfCalls=10, slidingWindowSize=10, 50% threshold)
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("boom"));
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldNotOpenCircuitBeforeMinimumNumberOfCalls() {
        // given
        CircuitBreaker circuitBreaker = config.identityCircuitBreaker();

        // when — only 9 failures, below minimumNumberOfCalls=10
        for (int i = 0; i < 9; i++) {
            circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("boom"));
        }

        // then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldTimeOutCallExceedingThreeSeconds() {
        // given
        TimeLimiter timeLimiter = config.identityTimeLimiter();
        var executor = Executors.newSingleThreadExecutor();

        // when / then — a 5s call must be interrupted at the 3s limit
        assertThatThrownBy(() -> timeLimiter.executeFutureSupplier(
                () -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "done";
                }, executor)))
                .isInstanceOf(TimeoutException.class);

        executor.shutdownNow();
    }
}
