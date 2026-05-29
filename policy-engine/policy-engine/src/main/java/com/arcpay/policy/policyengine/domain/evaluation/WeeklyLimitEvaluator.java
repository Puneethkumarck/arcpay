package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

@Component
public class WeeklyLimitEvaluator implements RuleEvaluator<PolicyRule.WeeklyLimit> {

    private static final String RULE_TYPE = "WEEKLY_LIMIT";

    @Override
    public Class<PolicyRule.WeeklyLimit> supportedType() {
        return PolicyRule.WeeklyLimit.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.WeeklyLimit rule, EvaluationContext context) {
        var current = context.spendingSummary().weeklyTotal();
        var requested = context.amount();
        var limit = rule.amount();
        var projected = current.add(requested);

        if (projected.compareTo(limit) <= 0) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .limit(limit)
                    .current(current)
                    .requested(requested)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .limit(limit)
                .current(current)
                .requested(requested)
                .message("Weekly spending %s + %s would exceed limit of %s".formatted(current, requested, limit))
                .build();
    }
}
