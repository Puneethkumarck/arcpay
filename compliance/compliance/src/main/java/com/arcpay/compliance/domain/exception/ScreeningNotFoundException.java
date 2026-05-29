package com.arcpay.compliance.domain.exception;

import java.util.UUID;

public class ScreeningNotFoundException extends RuntimeException {

    public ScreeningNotFoundException(UUID paymentId) {
        super("Screening result not found for payment: " + paymentId);
    }
}
