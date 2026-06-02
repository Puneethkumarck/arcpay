package com.arcpay.payment.paymentexecution.domain.model;

import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record ScreeningResultSignal(ScreeningVerdict verdict, Integer riskScore) {

    public ScreeningResultSignal {
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}
