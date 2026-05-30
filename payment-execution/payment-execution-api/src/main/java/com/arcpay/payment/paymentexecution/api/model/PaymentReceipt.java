package com.arcpay.payment.paymentexecution.api.model;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentReceipt(
        String onChainRef,
        Long blockNumber,
        Instant timestamp
) {}
