package com.arcpay.payment.paymentexecution.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PaymentRequested(
        UUID paymentId,
        UUID agentId,
        UUID ownerId,
        String idempotencyKey,
        String recipientAddress,
        BigDecimal amount,
        String currency,
        Instant requestedAt
) {

    public static final String TOPIC = "payment.requested";

    public PaymentRequested {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
