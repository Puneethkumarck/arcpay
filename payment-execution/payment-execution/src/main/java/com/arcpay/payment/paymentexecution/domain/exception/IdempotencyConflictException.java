package com.arcpay.payment.paymentexecution.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
