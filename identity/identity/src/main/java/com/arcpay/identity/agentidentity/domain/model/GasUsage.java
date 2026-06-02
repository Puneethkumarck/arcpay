package com.arcpay.identity.agentidentity.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record GasUsage(
        UUID id,
        UUID ownerId,
        UUID agentId,
        String operation,
        String txHash,
        long gasUsed,
        BigDecimal gasCostUsdc,
        Instant createdAt) {}
