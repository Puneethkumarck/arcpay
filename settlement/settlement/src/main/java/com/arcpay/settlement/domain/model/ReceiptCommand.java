package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record ReceiptCommand(
        UUID paymentId,
        String payerAgent,
        String payee,
        BigDecimal amount,
        String memo
) {

    public ReceiptCommand {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(payerAgent, "payerAgent must not be null");
        Objects.requireNonNull(payee, "payee must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
