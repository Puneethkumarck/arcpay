package com.arcpay.compliance.infrastructure.temporal;

import com.arcpay.compliance.infrastructure.sanctions.SanctionsSource;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "compliance.sanctions")
public record SanctionsIngestionProperties(
        String refreshCron,
        int stalenessWarnHours,
        int stalenessCriticalHours,
        int downloadTimeoutSeconds,
        List<SanctionsSource> sources,
        Map<SanctionsSource, String> sourceUrls) {}
