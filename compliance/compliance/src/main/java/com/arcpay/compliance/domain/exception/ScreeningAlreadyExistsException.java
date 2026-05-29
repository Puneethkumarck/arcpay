package com.arcpay.compliance.domain.exception;

import java.util.UUID;

public class ScreeningAlreadyExistsException extends RuntimeException {

    public ScreeningAlreadyExistsException(UUID paymentId) {
        super("Screening result already exists for payment: " + paymentId);
    }

    public ScreeningAlreadyExistsException(UUID paymentId, Throwable cause) {
        super("Screening result already exists for payment: " + paymentId, cause);
    }
}
