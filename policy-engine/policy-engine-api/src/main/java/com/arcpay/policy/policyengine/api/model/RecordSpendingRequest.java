package com.arcpay.policy.policyengine.api.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record RecordSpendingRequest(
        @NotNull UUID agentId,
        @NotNull UUID paymentId,
        @NotNull @DecimalMin("0.000001") BigDecimal amount,
        @NotNull @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String recipient,
        @NotNull Instant executedAt
) {}
