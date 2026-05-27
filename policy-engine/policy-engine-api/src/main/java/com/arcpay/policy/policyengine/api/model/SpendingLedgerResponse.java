package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SpendingLedgerResponse(
        UUID entryId,
        UUID agentId,
        UUID paymentId,
        BigDecimal amount,
        Instant createdAt
) {}
