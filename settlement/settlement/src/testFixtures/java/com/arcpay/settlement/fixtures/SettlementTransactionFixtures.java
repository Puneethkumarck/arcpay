package com.arcpay.settlement.fixtures;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SettlementTransactionFixtures {

    public static final UUID SOME_PAYMENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef111111");
    public static final String SOME_CIRCLE_TX_ID = "circle-tx-abc123";
    public static final String SOME_TX_HASH = "0xdeadbeef1234567890deadbeef1234567890deadbeef1234567890deadbeef12";
    public static final BigDecimal SOME_NETWORK_FEE = new BigDecimal("0.010000");
    public static final String SOME_ERROR_REASON = "insufficient on-chain funds";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-05-30T10:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-05-30T10:05:00Z");

    private SettlementTransactionFixtures() {
    }

    public static SettlementTransaction someSettlementTransaction(TransferState state) {
        return SettlementTransaction.builder()
                .paymentId(SOME_PAYMENT_ID)
                .circleTxId(SOME_CIRCLE_TX_ID)
                .txHash(SOME_TX_HASH)
                .state(state)
                .networkFee(SOME_NETWORK_FEE)
                .errorReason(null)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_UPDATED_AT)
                .build();
    }

    public static SettlementTransaction someFailedTransaction(TransferState state) {
        return someSettlementTransaction(state).toBuilder()
                .errorReason(SOME_ERROR_REASON)
                .build();
    }
}
