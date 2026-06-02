package com.arcpay.settlement.domain.model;

import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransferSubmission(String circleTxId, TransferState state) {

    public TransferSubmission {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
