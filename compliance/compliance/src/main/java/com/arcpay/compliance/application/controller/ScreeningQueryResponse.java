package com.arcpay.compliance.application.controller;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record ScreeningQueryResponse(
        UUID screeningId,
        UUID paymentId,
        UUID agentId,
        String recipientAddress,
        String verdict,
        int riskScore,
        List<ScreeningCheckResponse> checks,
        Instant timestamp,
        long durationMs
) {
}
