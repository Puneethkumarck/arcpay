package com.arcpay.policy.policyengine.api.model;

import com.arcpay.policy.policyengine.api.PolicyRule;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record PolicyResponse(
        UUID policyId,
        UUID agentId,
        int version,
        List<PolicyRule> rules,
        String policyHash,
        String status,
        Instant createdAt
) {}
