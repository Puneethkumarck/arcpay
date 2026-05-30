package com.arcpay.payment.paymentexecution.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Builder
public record CreatePaymentRequest(
        @NotNull(message = "Agent id is required") UUID agentId,
        @NotBlank(message = "Idempotency key is required")
        @Size(max = 255, message = "Idempotency key must not exceed 255 characters") String idempotencyKey,
        @NotBlank(message = "Recipient address is required") String recipientAddress,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be at least 0.000001")
        @Digits(integer = 30, fraction = 6, message = "Amount must not exceed 6 decimal places") BigDecimal amount,
        @NotBlank(message = "Currency is required") String currency,
        @Size(max = 256, message = "Memo must not exceed 256 characters") String memo,
        Map<String, String> metadata
) {
    public CreatePaymentRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
