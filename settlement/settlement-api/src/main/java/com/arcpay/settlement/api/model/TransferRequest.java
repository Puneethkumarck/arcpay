package com.arcpay.settlement.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record TransferRequest(
        @NotNull(message = "Payment id is required") UUID paymentId,
        @NotBlank(message = "Wallet id is required") String walletId,
        @NotBlank(message = "Recipient address is required") String recipientAddress,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be positive") BigDecimal amount,
        String memo
) {}
