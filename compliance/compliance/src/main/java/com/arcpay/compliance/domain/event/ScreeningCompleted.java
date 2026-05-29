package com.arcpay.compliance.domain.event;

import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.Verdict;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record ScreeningCompleted(
        UUID paymentId,
        UUID agentId,
        Verdict verdict,
        int riskScore,
        List<ScreeningCheck> checks,
        UUID listVersionId,
        Instant screenedAt
) {

    public static final String TOPIC = "screening.completed";

    public ScreeningCompleted {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(screenedAt, "screenedAt must not be null");
    }
}
