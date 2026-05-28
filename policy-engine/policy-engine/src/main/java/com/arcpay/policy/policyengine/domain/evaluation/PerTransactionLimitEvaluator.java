package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PerTransactionLimitEvaluator implements RuleEvaluator<PolicyRule.PerTransactionLimit> {

    private static final String RULE_TYPE = "PER_TX_LIMIT";

    @Override
    public Class<PolicyRule.PerTransactionLimit> supportedType() {
        return PolicyRule.PerTransactionLimit.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.PerTransactionLimit rule, EvaluationContext context) {
        var amount = context.amount();
        var limit = rule.amount();

        if (amount.compareTo(limit) <= 0) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .limit(limit)
                    .requested(amount)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .limit(limit)
                .requested(amount)
                .message("Amount %s exceeds per-transaction limit of %s".formatted(amount, limit))
                .build();
    }
}
