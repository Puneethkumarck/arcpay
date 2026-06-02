package com.arcpay.payment.paymentexecution.domain.model;

import com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged;
import java.util.Objects;
import lombok.Builder;

@Builder
public record PaymentTransition(Payment payment, PaymentStatusChanged event) {

    public PaymentTransition {
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(event, "event must not be null");
    }
}
