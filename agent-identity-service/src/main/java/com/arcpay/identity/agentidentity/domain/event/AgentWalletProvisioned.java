package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentWalletProvisioned(UUID agentId, String walletId, String walletAddress,
                                     Instant provisionedAt) {

    public static final String TOPIC = "agent.wallet-provisioned";

    public AgentWalletProvisioned {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(walletAddress, "walletAddress must not be null");
        Objects.requireNonNull(provisionedAt, "provisionedAt must not be null");
    }
}
