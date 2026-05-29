package com.arcpay.compliance.domain.event;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record ScreeningApproved(
        UUID paymentId,
        String reviewer,
        String reason,
        Instant decidedAt
) {

    public static final String TOPIC = "screening.approved";

    public ScreeningApproved {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(reviewer, "reviewer must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(decidedAt, "decidedAt must not be null");
    }
}
