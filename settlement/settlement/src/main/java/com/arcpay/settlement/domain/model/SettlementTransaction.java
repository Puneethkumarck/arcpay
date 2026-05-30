package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record SettlementTransaction(
        UUID paymentId,
        String circleTxId,
        String txHash,
        TransferState state,
        BigDecimal networkFee,
        String errorReason,
        Instant createdAt,
        Instant updatedAt
) {

    public SettlementTransaction {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public SettlementTransaction withCircleTxId(String circleTxId) {
        Objects.requireNonNull(circleTxId, "circleTxId must not be null");
        return toBuilder()
                .circleTxId(circleTxId)
                .updatedAt(Instant.now())
                .build();
    }

    public SettlementTransaction withState(TransferState state) {
        Objects.requireNonNull(state, "state must not be null");
        return toBuilder()
                .state(state)
                .updatedAt(Instant.now())
                .build();
    }

    public SettlementTransaction withOnChainResult(String txHash, BigDecimal networkFee, TransferState state) {
        Objects.requireNonNull(txHash, "txHash must not be null");
        Objects.requireNonNull(state, "state must not be null");
        return toBuilder()
                .txHash(txHash)
                .networkFee(networkFee)
                .state(state)
                .updatedAt(Instant.now())
                .build();
    }

    public SettlementTransaction withFailure(TransferState state, String errorReason) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(errorReason, "errorReason must not be null");
        return toBuilder()
                .state(state)
                .errorReason(errorReason)
                .updatedAt(Instant.now())
                .build();
    }
}
