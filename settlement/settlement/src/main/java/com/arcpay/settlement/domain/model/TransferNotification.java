package com.arcpay.settlement.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Builder;

@Builder(toBuilder = true)
public record TransferNotification(
        String circleTxId, TransferState state, String txHash, BigDecimal networkFee, String errorReason) {

    public TransferNotification {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        Objects.requireNonNull(state, "state must not be null");
    }
}
