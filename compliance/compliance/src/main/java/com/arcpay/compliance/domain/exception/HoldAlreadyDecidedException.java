package com.arcpay.compliance.domain.exception;

import com.arcpay.compliance.domain.model.ReviewState;

import java.util.UUID;

public class HoldAlreadyDecidedException extends RuntimeException {

    public HoldAlreadyDecidedException(UUID paymentId, ReviewState currentState) {
        super("Hold review for payment " + paymentId + " is already decided: " + currentState);
    }
}
