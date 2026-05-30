package com.arcpay.settlement.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Objects;

@Builder(toBuilder = true)
public record WalletBalance(
        String walletId,
        String tokenAddress,
        BigDecimal amount
) {

    public WalletBalance {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(tokenAddress, "tokenAddress must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
