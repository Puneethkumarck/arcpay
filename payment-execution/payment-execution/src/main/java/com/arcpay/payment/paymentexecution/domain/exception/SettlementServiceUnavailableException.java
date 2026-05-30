package com.arcpay.payment.paymentexecution.domain.exception;

public class SettlementServiceUnavailableException extends RuntimeException {

    public SettlementServiceUnavailableException(String message) {
        super(message);
    }

    public SettlementServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
