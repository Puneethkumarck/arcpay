package com.arcpay.settlement.api.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record TransferStatusResponse(
        UUID paymentId,
        String circleTxId,
        String txHash,
        String state,
        BigDecimal networkFee,
        String errorReason,
        Instant createdAt,
        Instant updatedAt
) {}
