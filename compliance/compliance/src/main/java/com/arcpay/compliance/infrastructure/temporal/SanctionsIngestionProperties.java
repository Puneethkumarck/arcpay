package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "compliance.sanctions")
public record SanctionsIngestionProperties(
        String refreshCron,
        int stalenessWarnHours,
        int stalenessCriticalHours,
        int downloadTimeoutSeconds,
        List<SanctionsSource> sources,
        Map<SanctionsSource, String> sourceUrls
) {
}
