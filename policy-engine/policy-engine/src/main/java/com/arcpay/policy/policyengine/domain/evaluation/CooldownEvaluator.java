package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CooldownEvaluator implements RuleEvaluator<PolicyRule.Cooldown> {

    private static final String RULE_TYPE = "COOLDOWN";

    @Override
    public Class<PolicyRule.Cooldown> supportedType() {
        return PolicyRule.Cooldown.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.Cooldown rule, EvaluationContext context) {
        var lastTransactionAt = context.spendingSummary().lastTransactionAt();

        if (lastTransactionAt == null) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .build();
        }

        var elapsedSeconds = Duration.between(lastTransactionAt, context.requestedAt()).toSeconds();

        if (elapsedSeconds >= rule.seconds()) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .message("Cooldown period not elapsed. Last transaction at %s, required cooldown %ds".formatted(
                        lastTransactionAt, rule.seconds()))
                .build();
    }
}
