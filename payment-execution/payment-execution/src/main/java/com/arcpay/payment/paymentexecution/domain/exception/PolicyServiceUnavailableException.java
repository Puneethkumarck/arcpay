package com.arcpay.payment.paymentexecution.domain.exception;

public class PolicyServiceUnavailableException extends RuntimeException {

    public PolicyServiceUnavailableException(String message) {
        super(message);
    }

    public PolicyServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
