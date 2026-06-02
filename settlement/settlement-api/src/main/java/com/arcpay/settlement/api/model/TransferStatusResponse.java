package com.arcpay.settlement.api.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TransferStatusResponse(
        UUID paymentId,
        String circleTxId,
        String txHash,
        String state,
        BigDecimal networkFee,
        String errorReason,
        Instant createdAt,
        Instant updatedAt) {}
