package com.arcpay.policy.policyengine.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PolicyViolationDetected(
        UUID evaluationId,
        UUID agentId,
        UUID policyId,
        String violatedRuleType,
        String message,
        BigDecimal requestedAmount,
        Instant detectedAt
) {

    public static final String TOPIC = "policy.violation-detected";
}
