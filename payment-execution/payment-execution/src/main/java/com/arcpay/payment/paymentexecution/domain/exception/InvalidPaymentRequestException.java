package com.arcpay.payment.paymentexecution.domain.exception;

public class InvalidPaymentRequestException extends RuntimeException {

    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
