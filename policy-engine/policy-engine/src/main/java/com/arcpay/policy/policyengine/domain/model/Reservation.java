package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record Reservation(
        UUID paymentId,
        UUID agentId,
        BigDecimal amount,
        String recipient,
        ReservationStatus status,
        Instant createdAt
) {

    public Reservation {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static Reservation held(UUID paymentId, UUID agentId, BigDecimal amount,
            String recipient, Instant createdAt) {
        return Reservation.builder()
                .paymentId(paymentId)
                .agentId(agentId)
                .amount(amount)
                .recipient(recipient)
                .status(ReservationStatus.HELD)
                .createdAt(createdAt)
                .build();
    }

    public Reservation commit() {
        return toBuilder().status(ReservationStatus.COMMITTED).build();
    }

    public Reservation release() {
        return toBuilder().status(ReservationStatus.RELEASED).build();
    }
}
