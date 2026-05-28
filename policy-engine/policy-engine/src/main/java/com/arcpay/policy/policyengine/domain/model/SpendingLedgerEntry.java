package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Builder(toBuilder = true)
public record SpendingLedgerEntry(
        UUID entryId,
        UUID agentId,
        UUID paymentId,
        BigDecimal amount,
        String recipient,
        Instant executedAt,
        Instant createdAt
) {

    public SpendingLedgerEntry {
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(executedAt, "executedAt must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
