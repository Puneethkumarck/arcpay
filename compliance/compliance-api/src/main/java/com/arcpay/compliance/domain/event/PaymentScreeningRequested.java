package com.arcpay.compliance.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PaymentScreeningRequested(
        UUID paymentId,
        UUID agentId,
        String recipientAddress,
        BigDecimal amount,
        String currency,
        Instant requestedAt
) {

    public static final String TOPIC = "screening.requested";

    public PaymentScreeningRequested {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
