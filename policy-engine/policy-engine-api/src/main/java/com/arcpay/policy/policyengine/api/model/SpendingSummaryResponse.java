package com.arcpay.policy.policyengine.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record SpendingSummaryResponse(
        UUID agentId,
        BigDecimal dailyTotal,
        BigDecimal weeklyTotal,
        BigDecimal monthlyTotal,
        int transactionCount24h,
        Instant lastTransactionAt) {}
