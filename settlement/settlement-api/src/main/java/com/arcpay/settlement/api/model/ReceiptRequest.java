package com.arcpay.settlement.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ReceiptRequest(
        @NotNull(message = "Payment id is required") UUID paymentId,
        @NotBlank(message = "Payer agent is required") String payerAgent,
        @NotBlank(message = "Payee is required") String payee,
        @NotNull(message = "Amount is required") BigDecimal amount,
        String memo
) {}
