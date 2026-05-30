package com.arcpay.payment.paymentexecution.domain.exception;

import java.util.UUID;

public class PaymentAccessDeniedException extends RuntimeException {

    public PaymentAccessDeniedException(UUID resourceId, UUID ownerId) {
        super("Resource " + resourceId + " does not belong to owner " + ownerId);
    }
}
