package com.arcpay.payment.paymentexecution.domain.exception;

import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;

import java.util.UUID;

public class IllegalPaymentTransitionException extends RuntimeException {

    public IllegalPaymentTransitionException(UUID paymentId, PaymentStatus fromStatus, PaymentStatus toStatus) {
        super("Payment " + paymentId + " cannot transition from " + fromStatus + " to " + toStatus);
    }
}
