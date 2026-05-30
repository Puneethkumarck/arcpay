package com.arcpay.payment.paymentexecution.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record CreatePaymentRequest(
        @NotBlank(message = "Recipient address is required") String recipientAddress,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.000001", message = "Amount must be positive") BigDecimal amount,
        @NotBlank(message = "Currency is required") String currency,
        @Size(max = 256, message = "Memo must not exceed 256 characters") String memo,
        Map<String, String> metadata
) {
    public CreatePaymentRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
