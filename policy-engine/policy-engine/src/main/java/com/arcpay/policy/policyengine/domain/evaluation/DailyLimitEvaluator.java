package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

@Component
public class DailyLimitEvaluator implements RuleEvaluator<PolicyRule.DailyLimit> {

    private static final String RULE_TYPE = "DAILY_LIMIT";

    @Override
    public Class<PolicyRule.DailyLimit> supportedType() {
        return PolicyRule.DailyLimit.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.DailyLimit rule, EvaluationContext context) {
        var current = context.spendingSummary().dailyTotal();
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
                .message("Daily spending %s + %s would exceed limit of %s".formatted(current, requested, limit))
                .build();
    }
}
