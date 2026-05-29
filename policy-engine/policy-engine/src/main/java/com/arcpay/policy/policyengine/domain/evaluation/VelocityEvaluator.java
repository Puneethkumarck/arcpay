package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class VelocityEvaluator implements RuleEvaluator<PolicyRule.Velocity> {

    private static final String RULE_TYPE = "VELOCITY";

    @Override
    public Class<PolicyRule.Velocity> supportedType() {
        return PolicyRule.Velocity.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.Velocity rule, EvaluationContext context) {
        var count = context.spendingSummary().velocityCount();
        var max = rule.maxTransactions();

        if (count < max) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .limit(BigDecimal.valueOf(max))
                    .current(BigDecimal.valueOf(count))
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .limit(BigDecimal.valueOf(max))
                .current(BigDecimal.valueOf(count))
                .message("Transaction count %d in last %d minutes would exceed limit of %d".formatted(
                        count, rule.periodMinutes(), max))
                .build();
    }
}
