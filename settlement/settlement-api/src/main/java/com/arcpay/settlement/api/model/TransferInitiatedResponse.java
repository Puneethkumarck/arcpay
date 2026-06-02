package com.arcpay.settlement.api.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record TransferInitiatedResponse(UUID paymentId, String circleTxId, String state) {}
