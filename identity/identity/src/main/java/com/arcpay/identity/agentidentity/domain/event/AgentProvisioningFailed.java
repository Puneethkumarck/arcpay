package com.arcpay.identity.agentidentity.domain.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentProvisioningFailed(UUID agentId, String failedStep, String reason,
                                      Instant failedAt) {

    public static final String TOPIC = "agent.provisioning-failed";

    public AgentProvisioningFailed {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(failedStep, "failedStep must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(failedAt, "failedAt must not be null");
    }
}
