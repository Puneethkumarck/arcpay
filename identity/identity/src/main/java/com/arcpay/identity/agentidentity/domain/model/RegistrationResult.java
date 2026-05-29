package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record RegistrationResult(
        String txHash,
        long blockNumber
) {
}
