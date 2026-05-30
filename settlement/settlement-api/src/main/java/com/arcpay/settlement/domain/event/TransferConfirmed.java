package com.arcpay.settlement.domain.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record TransferConfirmed(
        UUID paymentId,
        String txHash,
        BigDecimal networkFee,
        Instant confirmedAt
) implements SettlementEvent {

    public static final String TOPIC = "transfer.confirmed";

    public TransferConfirmed {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(txHash, "txHash must not be null");
        Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
    }
}
