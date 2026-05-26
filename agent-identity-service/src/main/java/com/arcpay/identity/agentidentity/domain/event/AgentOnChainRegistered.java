package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentOnChainRegistered(UUID agentId, String txHash, long blockNumber,
                                     Instant registeredAt) {

    public static final String TOPIC = "agent.on-chain-registered";

    public AgentOnChainRegistered {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(txHash, "txHash must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
    }
}
