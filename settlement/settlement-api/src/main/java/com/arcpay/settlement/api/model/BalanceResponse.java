package com.arcpay.settlement.api.model;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BalanceResponse(
        String agentId, String walletId, String tokenAddress, BigDecimal amount, String currency) {}
