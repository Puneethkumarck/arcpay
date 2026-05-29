package com.arcpay.policy.policyengine.domain.event;

import java.time.Instant;
import java.util.UUID;

public record PolicyCreated(
        UUID policyId,
        UUID agentId,
        UUID ownerId,
        int version,
        String policyHash,
        Instant createdAt
) {

    public static final String TOPIC = "policy.created";
}
