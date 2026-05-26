package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record GasUsage(
        UUID id,
        UUID ownerId,
        UUID agentId,
        String operation,
        String txHash,
        long gasUsed,
        BigDecimal gasCostUsdc,
        Instant createdAt
) {
}
