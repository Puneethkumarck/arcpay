package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record WalletCreationResult(
        String walletId,
        String walletAddress
) {
}
