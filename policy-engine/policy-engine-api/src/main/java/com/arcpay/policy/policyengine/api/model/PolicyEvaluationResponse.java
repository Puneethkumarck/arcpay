package com.arcpay.policy.policyengine.api.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PolicyEvaluationResponse(
        UUID evaluationId,
        UUID agentId,
        UUID policyId,
        String verdict,
        List<RuleResultResponse> ruleResults,
        boolean dryRun,
        Instant evaluatedAt,
        long durationMs) {}
