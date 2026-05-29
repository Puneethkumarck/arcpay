package com.arcpay.compliance.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record ScreeningResult(
        UUID screeningId,
        UUID paymentId,
        UUID agentId,
        String recipientAddress,
        Verdict verdict,
        int riskScore,
        List<ScreeningCheck> checks,
        UUID listVersionId,
        Instant screenedAt,
        long durationMs
) {

    public ScreeningResult {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}
