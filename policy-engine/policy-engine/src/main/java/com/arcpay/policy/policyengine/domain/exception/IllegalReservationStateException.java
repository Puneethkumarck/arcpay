package com.arcpay.policy.policyengine.domain.exception;

import com.arcpay.policy.policyengine.domain.model.ReservationStatus;

import java.util.UUID;

public class IllegalReservationStateException extends RuntimeException {

    public IllegalReservationStateException(UUID paymentId, ReservationStatus current, String operation) {
        super("Cannot %s reservation for payment %s in state %s".formatted(operation, paymentId, current));
    }
}
