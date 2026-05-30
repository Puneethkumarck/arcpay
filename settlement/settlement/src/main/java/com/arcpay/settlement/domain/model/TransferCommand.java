package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record TransferCommand(
        UUID paymentId,
        String walletId,
        String recipientAddress,
        BigDecimal amount,
        String memo
) {

    public TransferCommand {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
