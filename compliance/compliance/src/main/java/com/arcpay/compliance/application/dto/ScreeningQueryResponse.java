package com.arcpay.compliance.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

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
        long durationMs) {}
