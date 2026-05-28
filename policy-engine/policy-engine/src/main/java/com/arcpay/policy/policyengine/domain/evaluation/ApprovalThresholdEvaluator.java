package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalThresholdEvaluator implements RuleEvaluator<PolicyRule.ApprovalThreshold> {

    private static final String RULE_TYPE = "APPROVAL_THRESHOLD";

    @Override
    public Class<PolicyRule.ApprovalThreshold> supportedType() {
        return PolicyRule.ApprovalThreshold.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.ApprovalThreshold rule, EvaluationContext context) {
        var amount = context.amount();
        var threshold = rule.amount();

        if (amount.compareTo(threshold) <= 0) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .limit(threshold)
                    .requested(amount)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.REQUIRES_APPROVAL)
                .limit(threshold)
                .requested(amount)
                .message("Amount %s exceeds approval threshold of %s, owner approval required".formatted(amount, threshold))
                .build();
    }
}
