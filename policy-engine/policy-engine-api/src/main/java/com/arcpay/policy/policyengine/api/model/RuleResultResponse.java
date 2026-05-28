package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RuleResultResponse(
        String ruleType,
        String verdict,
        BigDecimal limit,
        BigDecimal current,
        BigDecimal requested,
        String message
) {}
