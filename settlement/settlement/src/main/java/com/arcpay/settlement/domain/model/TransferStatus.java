package com.arcpay.settlement.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransferStatus(
        String circleTxId, String txHash, TransferState state, BigDecimal networkFee, String errorReason) {

    public TransferStatus {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
