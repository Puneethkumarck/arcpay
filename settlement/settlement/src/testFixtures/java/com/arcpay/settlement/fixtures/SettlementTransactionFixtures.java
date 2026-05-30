package com.arcpay.settlement.fixtures;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferCommand;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.model.WalletBalance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SettlementTransactionFixtures {

    public static final UUID SOME_PAYMENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef111111");
    public static final String SOME_WALLET_ID = "wallet-abc-123";
    public static final String SOME_RECIPIENT_ADDRESS = "0x000000000000000000000000000000000000dead";
    public static final BigDecimal SOME_TRANSFER_AMOUNT = new BigDecimal("25.00");
    public static final String SOME_CIRCLE_TX_ID = "circle-tx-abc123";
    public static final String SOME_TX_HASH = "0xdeadbeef1234567890deadbeef1234567890deadbeef1234567890deadbeef12";
    public static final BigDecimal SOME_NETWORK_FEE = new BigDecimal("0.010000");
    public static final String SOME_ERROR_REASON = "insufficient on-chain funds";
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-05-30T10:00:00Z");
    public static final Instant SOME_UPDATED_AT = Instant.parse("2026-05-30T10:05:00Z");
    public static final String SOME_USDC_TOKEN_ADDRESS = "0x3600000000000000000000000000000000000000";
    public static final BigDecimal SOME_BALANCE_AMOUNT = new BigDecimal("120.500000");

    private SettlementTransactionFixtures() {
    }

    public static WalletBalance someWalletBalance() {
        return new WalletBalance(SOME_WALLET_ID, SOME_USDC_TOKEN_ADDRESS, SOME_BALANCE_AMOUNT);
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

    public static SettlementTransaction someTransactionWith(UUID paymentId, TransferState state) {
        return someSettlementTransaction(state).toBuilder()
                .paymentId(paymentId)
                .build();
    }

    public static TransferCommand someTransferCommand() {
        return TransferCommand.builder()
                .paymentId(SOME_PAYMENT_ID)
                .walletId(SOME_WALLET_ID)
                .recipientAddress(SOME_RECIPIENT_ADDRESS)
                .amount(SOME_TRANSFER_AMOUNT)
                .memo("invoice-42")
                .build();
    }
}
