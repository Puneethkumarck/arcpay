package com.arcpay.payment.paymentexecution.domain.exception;

import java.util.UUID;

public class MissingTransferHashException extends RuntimeException {

    public MissingTransferHashException(UUID paymentId) {
        super("Transfer submitted without a transaction hash for payment: " + paymentId);
    }
}
