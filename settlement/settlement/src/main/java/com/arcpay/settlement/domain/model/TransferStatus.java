package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;

@Builder(toBuilder = true)
public record TransferStatus(
        String circleTxId,
        String txHash,
        TransferState state,
        BigDecimal networkFee,
        String errorReason
) {

    public TransferStatus {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
