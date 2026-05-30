package com.arcpay.payment.paymentexecution.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentResponse(
        UUID paymentId,
        String idempotencyKey,
        UUID agentId,
        String status,
        String from,
        String to,
        BigDecimal amount,
        String currency,
        String chain,
        String transactionHash,
        PolicyResult policyResult,
        ComplianceResult complianceResult,
        PaymentReceipt receipt,
        Instant createdAt,
        Instant completedAt
) {}
