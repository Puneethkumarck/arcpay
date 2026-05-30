package com.arcpay.settlement.fixtures;

import com.arcpay.settlement.domain.model.ReceiptCommand;

import java.math.BigDecimal;
import java.util.UUID;

public final class ReceiptCommandFixtures {

    public static final UUID SOME_RECEIPT_PAYMENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef222222");
    public static final String SOME_PAYER_ADDRESS = "0x1111111111111111111111111111111111111111";
    public static final String SOME_PAYEE_ADDRESS = "0x2222222222222222222222222222222222222222";
    public static final BigDecimal SOME_RECEIPT_AMOUNT = new BigDecimal("12.500000");
    public static final String SOME_MEMO = "invoice-4815";

    private ReceiptCommandFixtures() {
    }

    public static ReceiptCommand someReceiptCommand() {
        return ReceiptCommand.builder()
                .paymentId(SOME_RECEIPT_PAYMENT_ID)
                .payerAgent(SOME_PAYER_ADDRESS)
                .payee(SOME_PAYEE_ADDRESS)
                .amount(SOME_RECEIPT_AMOUNT)
                .memo(SOME_MEMO)
                .build();
    }

    public static ReceiptCommand someReceiptCommandWithoutMemo() {
        return someReceiptCommand().toBuilder()
                .memo(null)
                .build();
    }
}
