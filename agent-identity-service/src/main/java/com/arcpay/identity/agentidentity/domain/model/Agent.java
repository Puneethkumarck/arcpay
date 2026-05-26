package com.arcpay.identity.agentidentity.domain.model;

import com.arcpay.identity.agentidentity.domain.exception.AgentNotInExpectedStateException;
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
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(walletAddress, "walletAddress must not be null");
        return toBuilder()
                .walletId(walletId)
                .walletAddress(walletAddress)
                .status(AgentStatus.WALLET_READY)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent withOnChainRegistration(String txHash) {
        Objects.requireNonNull(txHash, "txHash must not be null");
        return toBuilder()
                .onChainTxHash(txHash)
                .status(AgentStatus.ACTIVE)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent withFailure(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        return toBuilder()
                .failureReason(reason)
                .status(AgentStatus.FAILED)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent deactivate() {
        if (status != AgentStatus.ACTIVE) {
            throw new AgentNotInExpectedStateException(agentId, status, AgentStatus.ACTIVE);
        }
        return toBuilder()
                .status(AgentStatus.SUSPENDED)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent reactivate() {
        if (status != AgentStatus.SUSPENDED) {
            throw new AgentNotInExpectedStateException(agentId, status, AgentStatus.SUSPENDED);
        }
        return toBuilder()
                .status(AgentStatus.ACTIVE)
                .updatedAt(Instant.now())
                .build();
    }

    public Agent updateMetadata(String name, String purpose, String metadataHash) {
        if (status == AgentStatus.PROVISIONING || status == AgentStatus.FAILED) {
            throw new AgentNotInExpectedStateException(agentId, status, AgentStatus.ACTIVE);
        }
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(metadataHash, "metadataHash must not be null");
        return toBuilder()
                .name(name)
                .purpose(purpose)
                .metadataHash(metadataHash)
                .updatedAt(Instant.now())
                .build();
    }
}
