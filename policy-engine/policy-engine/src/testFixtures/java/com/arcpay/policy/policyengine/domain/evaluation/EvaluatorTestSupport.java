package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class EvaluatorTestSupport {

    private EvaluatorTestSupport() {}

    private static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000001");
    private static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    private static final UUID SOME_POLICY_ID = UUID.fromString("019576a0-0000-7000-8000-000000000003");
    private static final Instant SOME_REQUESTED_AT = Instant.parse("2026-01-07T10:00:00Z");

    public static final String SOME_RECIPIENT = "0xrecipient";

    public static EvaluationContext contextWith(BigDecimal amount, String recipientAddress) {
        return contextWith(amount, recipientAddress, emptySpending());
    }

    public static EvaluationContext contextWith(BigDecimal amount, SpendingSummary spendingSummary) {
        return contextWith(amount, SOME_RECIPIENT, spendingSummary);
    }

    public static EvaluationContext contextWith(BigDecimal amount, String recipientAddress, SpendingSummary spendingSummary) {
        return EvaluationContext.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .policyId(SOME_POLICY_ID)
                .recipientAddress(recipientAddress)
                .amount(amount)
                .requestedAt(SOME_REQUESTED_AT)
                .dryRun(false)
                .spendingSummary(spendingSummary)
                .build();
    }

    public static EvaluationContext contextAt(BigDecimal amount, Instant requestedAt, SpendingSummary spendingSummary) {
        return EvaluationContext.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .policyId(SOME_POLICY_ID)
                .recipientAddress(SOME_RECIPIENT)
                .amount(amount)
                .requestedAt(requestedAt)
                .dryRun(false)
                .spendingSummary(spendingSummary)
                .build();
    }

    public static SpendingSummary emptySpending() {
        return SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(0)
                .build();
    }
}
