package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class SpendingFixtures {

    private SpendingFixtures() {}

    public static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000010");
    public static final UUID SOME_PAYMENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000011");
    public static final UUID SOME_ENTRY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000012");
    public static final BigDecimal SOME_AMOUNT = new BigDecimal("100.000000");
    public static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    public static final Instant SOME_EXECUTED_AT = Instant.parse("2026-01-01T12:00:00Z");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-01-01T12:00:01Z");
    public static final int SOME_VELOCITY_MINUTES = 60;

    public static final SpendingLedgerEntry SOME_LEDGER_ENTRY = SpendingLedgerEntry.builder()
            .entryId(SOME_ENTRY_ID)
            .agentId(SOME_AGENT_ID)
            .paymentId(SOME_PAYMENT_ID)
            .amount(SOME_AMOUNT)
            .recipient(SOME_RECIPIENT)
            .executedAt(SOME_EXECUTED_AT)
            .createdAt(SOME_CREATED_AT)
            .build();

    public static final SpendingSummary SOME_SPENDING_SUMMARY = SpendingSummary.builder()
            .dailyTotal(new BigDecimal("250.000000"))
            .weeklyTotal(new BigDecimal("750.000000"))
            .monthlyTotal(new BigDecimal("1500.000000"))
            .velocityCount(3)
            .lastTransactionAt(SOME_EXECUTED_AT)
            .build();
}
