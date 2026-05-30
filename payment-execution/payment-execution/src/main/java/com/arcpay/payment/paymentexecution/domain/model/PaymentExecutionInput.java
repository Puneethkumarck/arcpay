package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record PaymentExecutionInput(
        UUID paymentId,
        UUID agentId,
        String walletId,
        String recipient,
        BigDecimal amount,
        String memo
) {

    public PaymentExecutionInput {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
