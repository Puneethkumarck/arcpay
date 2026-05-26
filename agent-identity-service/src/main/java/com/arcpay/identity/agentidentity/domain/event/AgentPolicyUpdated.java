package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentPolicyUpdated(UUID agentId, String policyHash, Instant updatedAt) {

    public static final String TOPIC = "agent.policy-updated";

    public AgentPolicyUpdated {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(policyHash, "policyHash must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
