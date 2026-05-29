package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;

@Slf4j
@Component
@RequiredArgsConstructor
class StalenessMetricsPublisher {

    private static final String STALENESS_GAUGE = "compliance.sanctions.staleness.hours";

    private final SanctionsRefreshTracker refreshTracker;
    private final SanctionsIngestionProperties properties;
    private final MeterRegistry meterRegistry;
    private final Map<SanctionsSource, AtomicLong> gauges = new EnumMap<>(SanctionsSource.class);

    @Scheduled(fixedDelayString = "${compliance.sanctions.staleness-check-interval-ms:3600000}")
    void publishStaleness() {
        for (var source : properties.sources()) {
            var staleHours = stalenessHours(source);
            gaugeFor(source).set(staleHours);
            evaluate(source, staleHours);
        }
    }

    private long stalenessHours(SanctionsSource source) {
        return refreshTracker.lastSuccessfulRefresh(source)
                .map(last -> Duration.between(last, Instant.now()).toHours())
                .orElse(Long.MAX_VALUE);
    }

    private void evaluate(SanctionsSource source, long staleHours) {
        if (source == OFAC_SDN && staleHours >= properties.stalenessCriticalHours()) {
            log.error("CRITICAL: OFAC SDN sanctions list stale for {}h (threshold {}h); serving last-good list",
                    staleHours, properties.stalenessCriticalHours());
        } else if (staleHours >= properties.stalenessWarnHours()) {
            log.warn("WARNING: sanctions source {} stale for {}h (threshold {}h); serving last-good list",
                    source, staleHours, properties.stalenessWarnHours());
        }
    }

    private AtomicLong gaugeFor(SanctionsSource source) {
        return gauges.computeIfAbsent(source, s -> {
            var holder = new AtomicLong();
            meterRegistry.gauge(STALENESS_GAUGE, Tags.of("source", s.name()), holder, AtomicLong::doubleValue);
            return holder;
        });
    }
}
