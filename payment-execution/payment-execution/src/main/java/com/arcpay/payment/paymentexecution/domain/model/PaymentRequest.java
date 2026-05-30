package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PaymentRequest(
        UUID agentId,
        UUID ownerId,
        String idempotencyKey,
        String recipientAddress,
        BigDecimal amount,
        String currency,
        String memo,
        Map<String, String> metadata
) {

    public PaymentRequest {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
