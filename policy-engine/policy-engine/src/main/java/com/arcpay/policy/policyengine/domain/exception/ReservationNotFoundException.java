package com.arcpay.policy.policyengine.domain.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(UUID paymentId) {
        super("No reservation found for payment " + paymentId);
    }
}
