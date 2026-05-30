package com.arcpay.payment.paymentexecution.domain.event;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PaymentStatusChanged(
        UUID paymentId,
        UUID agentId,
        String status,
        String previousStatus,
        String transactionHash,
        Instant changedAt
) {

    public static final String TOPIC = "payment.status-changed";

    public PaymentStatusChanged {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
    }
}
