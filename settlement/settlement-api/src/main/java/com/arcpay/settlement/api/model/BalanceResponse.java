package com.arcpay.settlement.api.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BalanceResponse(
        String agentId,
        String walletId,
        String tokenAddress,
        BigDecimal amount,
        String currency
) {}
