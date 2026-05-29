package com.arcpay.policy.policyengine.infrastructure.client.identity;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .build();
        return CircuitBreakerRegistry.of(config).circuitBreaker("identity-service");
    }
}
