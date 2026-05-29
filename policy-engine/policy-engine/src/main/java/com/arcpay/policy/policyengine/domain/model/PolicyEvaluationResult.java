package com.arcpay.policy.policyengine.domain.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
public record PolicyEvaluationResult(
        UUID evaluationId,
        UUID agentId,
        UUID policyId,
        PolicyVerdict verdict,
        List<RuleEvaluationResult> ruleResults,
        BigDecimal requestedAmount,
        String recipientAddress,
        boolean dryRun,
        Instant evaluatedAt,
        long durationMs
) {

    public PolicyEvaluationResult {
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(policyId, "policyId must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
        Objects.requireNonNull(ruleResults, "ruleResults must not be null");
        Objects.requireNonNull(requestedAmount, "requestedAmount must not be null");
        Objects.requireNonNull(recipientAddress, "recipientAddress must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        ruleResults = List.copyOf(ruleResults);
    }
}
