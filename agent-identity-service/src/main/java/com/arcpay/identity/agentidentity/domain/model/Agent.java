package com.arcpay.identity.agentidentity.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Objects;
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

    public Agent {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(metadataHash, "metadataHash must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

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
