package com.arcpay.compliance.fixtures;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

public final class CircuitBreakerFixtures {

    private CircuitBreakerFixtures() {}

    public static CircuitBreaker identityCallBreaker(CircuitBreakerRegistry registry) {
        return registry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("IdentityServiceClient")
                        && b.getMetrics().getNumberOfBufferedCalls() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No IdentityServiceClient circuit breaker recorded any calls — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }

    public static CircuitBreaker getAgentBreaker(CircuitBreakerRegistry registry) {
        return registry.getAllCircuitBreakers().stream()
                .filter(b -> b.getName().startsWith("IdentityServiceClient")
                        && b.getName().contains("getAgent"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No IdentityServiceClient getAgent circuit breaker was registered — "
                                + "the OpenFeign circuit-breaker integration did not engage"));
    }
}
