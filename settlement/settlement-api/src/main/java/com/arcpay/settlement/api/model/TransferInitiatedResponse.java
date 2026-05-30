package com.arcpay.settlement.api.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TransferInitiatedResponse(
        UUID paymentId,
        String circleTxId,
        String state
) {}
