package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

import java.util.Objects;

@Builder(toBuilder = true)
public record ScreeningResultSignal(
        ScreeningVerdict verdict,
        Integer riskScore
) {

    public ScreeningResultSignal {
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}
