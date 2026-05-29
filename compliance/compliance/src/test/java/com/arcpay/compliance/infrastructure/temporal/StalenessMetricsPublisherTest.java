package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.OFAC_SDN;
import static com.arcpay.compliance.infrastructure.sanctions.SanctionsSource.UN;
import static org.assertj.core.api.Assertions.assertThat;

class StalenessMetricsPublisherTest {

    private final SanctionsRefreshTracker refreshTracker = new SanctionsRefreshTracker();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final SanctionsIngestionProperties properties = new SanctionsIngestionProperties(
            "0 0 */6 * * *", 12, 24, 30, List.of(OFAC_SDN, UN), Map.of());
    private final StalenessMetricsPublisher publisher =
            new StalenessMetricsPublisher(refreshTracker, properties, meterRegistry);

    @Test
    void shouldPublishStalenessGaugePerConfiguredSource() {
        // given
        refreshTracker.recordSuccess(OFAC_SDN, Instant.now());
        refreshTracker.recordSuccess(UN, Instant.now());

        // when
        publisher.publishStaleness();

        // then
        var gaugedSources = meterRegistry.find("compliance.sanctions.staleness.hours").gauges().stream()
                .map(gauge -> gauge.getId().getTag("source"))
                .sorted()
                .toList();
        assertThat(gaugedSources).containsExactly(OFAC_SDN.name(), UN.name());
    }

    @Test
    void shouldReportMaximumStalenessWhenSourceNeverRefreshed() {
        // when
        publisher.publishStaleness();

        // then
        var staleHours = meterRegistry.find("compliance.sanctions.staleness.hours")
                .tag("source", OFAC_SDN.name())
                .gauge()
                .value();
        assertThat(staleHours).isEqualTo((double) Long.MAX_VALUE);
    }
}
