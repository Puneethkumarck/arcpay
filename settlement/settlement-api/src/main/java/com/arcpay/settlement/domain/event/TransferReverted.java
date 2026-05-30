package com.arcpay.settlement.domain.event;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record TransferReverted(
        UUID paymentId,
        String reason,
        Instant revertedAt
) implements SettlementEvent {

    public static final String TOPIC = "transfer.reverted";

    public TransferReverted {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(revertedAt, "revertedAt must not be null");
    }
}
