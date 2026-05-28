package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record PolicyEvaluationResponse(
        UUID evaluationId,
        UUID agentId,
        UUID policyId,
        String verdict,
        List<RuleResultResponse> ruleResults,
        boolean dryRun,
        Instant evaluatedAt,
        long durationMs
) {}
