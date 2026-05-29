package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
public record AgentInfo(
        UUID agentId,
        UUID ownerId,
        String status,
        String policyHash
) {
}
