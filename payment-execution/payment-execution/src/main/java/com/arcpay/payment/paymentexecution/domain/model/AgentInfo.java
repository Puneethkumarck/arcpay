package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AgentInfo(
        UUID agentId,
        UUID ownerId,
        String status,
        String walletId,
        String walletAddress
) {
}
