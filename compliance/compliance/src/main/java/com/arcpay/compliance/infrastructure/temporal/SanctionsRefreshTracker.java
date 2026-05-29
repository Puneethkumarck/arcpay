package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SanctionsRefreshTracker {

    private final Map<SanctionsSource, Instant> lastSuccessfulRefresh = new ConcurrentHashMap<>();

    public void recordSuccess(SanctionsSource source, Instant refreshedAt) {
        lastSuccessfulRefresh.put(source, refreshedAt);
    }

    public Optional<Instant> lastSuccessfulRefresh(SanctionsSource source) {
        return Optional.ofNullable(lastSuccessfulRefresh.get(source));
    }

    public Map<SanctionsSource, Instant> snapshot() {
        return Map.copyOf(lastSuccessfulRefresh);
    }
}
