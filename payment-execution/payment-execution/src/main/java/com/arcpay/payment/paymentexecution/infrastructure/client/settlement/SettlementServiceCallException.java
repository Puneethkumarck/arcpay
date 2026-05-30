package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

class SettlementServiceCallException extends RuntimeException {

    SettlementServiceCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
