package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Builder(toBuilder = true)
public record SpendingSummary(
        BigDecimal dailyTotal,
        BigDecimal weeklyTotal,
        BigDecimal monthlyTotal,
        int velocityCount,
        Instant lastTransactionAt
) {

    public SpendingSummary {
        Objects.requireNonNull(dailyTotal, "dailyTotal must not be null");
        Objects.requireNonNull(weeklyTotal, "weeklyTotal must not be null");
        Objects.requireNonNull(monthlyTotal, "monthlyTotal must not be null");
    }
}
