package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record EvaluationContext(
        UUID agentId,
        UUID ownerId,
        UUID policyId,
        String recipientAddress,
        BigDecimal amount,
        Instant requestedAt,
        boolean dryRun,
        SpendingSummary spendingSummary
) {

    public EvaluationContext {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
    }
}
