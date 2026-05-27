package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SpendingSummaryResponse(
        UUID agentId,
        BigDecimal dailyTotal,
        BigDecimal weeklyTotal,
        BigDecimal monthlyTotal,
        int transactionCount24h,
        Instant lastTransactionAt
) {}
