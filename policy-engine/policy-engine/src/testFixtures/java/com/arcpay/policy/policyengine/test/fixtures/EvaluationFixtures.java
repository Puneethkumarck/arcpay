package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;

import java.math.BigDecimal;
import java.time.Instant;

public final class EvaluationFixtures {

    private EvaluationFixtures() {}

    public static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    public static final BigDecimal SOME_AMOUNT = new BigDecimal("30.00");
    public static final Instant SOME_REQUESTED_AT = Instant.parse("2026-01-07T10:00:00Z");

    public static final RuleEvaluationResult PASS_PER_TX = RuleEvaluationResult.builder()
            .ruleType("PER_TX_LIMIT")
            .verdict(RuleVerdict.PASS)
            .limit(new BigDecimal("100.00"))
            .requested(SOME_AMOUNT)
            .build();

    public static final RuleEvaluationResult FAIL_PER_TX = RuleEvaluationResult.builder()
            .ruleType("PER_TX_LIMIT")
            .verdict(RuleVerdict.FAIL)
            .limit(new BigDecimal("25.00"))
            .requested(SOME_AMOUNT)
            .message("Amount 30.00 exceeds per-transaction limit of 25.00")
            .build();

    public static final RuleEvaluationResult REQUIRES_APPROVAL_THRESHOLD = RuleEvaluationResult.builder()
            .ruleType("APPROVAL_THRESHOLD")
            .verdict(RuleVerdict.REQUIRES_APPROVAL)
            .limit(new BigDecimal("20.00"))
            .requested(SOME_AMOUNT)
            .message("Amount 30.00 exceeds approval threshold of 20.00, owner approval required")
            .build();

    public static final RuleEvaluationResult PASS_DAILY = RuleEvaluationResult.builder()
            .ruleType("DAILY_LIMIT")
            .verdict(RuleVerdict.PASS)
            .limit(new BigDecimal("1000.00"))
            .current(new BigDecimal("250.000000"))
            .requested(SOME_AMOUNT)
            .build();

    public static final RuleEvaluationResult FAIL_DAILY = RuleEvaluationResult.builder()
            .ruleType("DAILY_LIMIT")
            .verdict(RuleVerdict.FAIL)
            .limit(new BigDecimal("100.00"))
            .current(new BigDecimal("250.000000"))
            .requested(SOME_AMOUNT)
            .message("Daily total 280.00 would exceed limit of 100.00")
            .build();
}
