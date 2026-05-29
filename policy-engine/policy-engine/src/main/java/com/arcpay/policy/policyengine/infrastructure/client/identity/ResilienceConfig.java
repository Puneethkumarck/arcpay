package com.arcpay.policy.policyengine.infrastructure.client.identity;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
class ResilienceConfig {

    @Bean
    CircuitBreaker identityCircuitBreaker() {
        var config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                // minimumNumberOfCalls must be <= slidingWindowSize, otherwise the
                // failure rate is never evaluated and the breaker can never open.
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .build();
        return CircuitBreakerRegistry.of(config).circuitBreaker("identity-service");
    }

    @Bean
    TimeLimiter identityTimeLimiter() {
        var config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .cancelRunningFuture(true)
                .build();
        return TimeLimiterRegistry.of(config).timeLimiter("identity-service");
    }
}
