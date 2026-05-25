package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
public record Agent(
        UUID agentId,
        UUID ownerId,
        String name,
        String purpose,
        AgentStatus status,
        String walletId,
        String walletAddress,
        String onChainTxHash,
        String policyHash,
        String metadataHash,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public Agent withWallet(String walletId, String walletAddress) {
        return toBuilder()
                .walletId(walletId)
                .walletAddress(walletAddress)
                .status(AgentStatus.WALLET_READY)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent withOnChainRegistration(String txHash) {
        return toBuilder()
                .onChainTxHash(txHash)
                .status(AgentStatus.ACTIVE)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent withFailure(String reason) {
        return toBuilder()
                .failureReason(reason)
                .status(AgentStatus.FAILED)
                .updatedAt(Instant.now())
                .build();
    }
}
