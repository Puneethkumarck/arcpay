package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.util.Objects;

@Builder(toBuilder = true)
public record TransferSubmission(
        String circleTxId,
        TransferState state
) {

    public TransferSubmission {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
