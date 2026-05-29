package com.arcpay.compliance.domain.exception;

import java.util.UUID;

public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(UUID paymentId) {
        super("Hold review not found for payment: " + paymentId);
    }
}
