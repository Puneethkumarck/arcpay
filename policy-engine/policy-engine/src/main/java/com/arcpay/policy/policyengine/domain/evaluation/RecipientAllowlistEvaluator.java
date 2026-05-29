package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class RecipientAllowlistEvaluator implements RuleEvaluator<PolicyRule.RecipientAllowlist> {

    private static final String RULE_TYPE = "RECIPIENT_ALLOWLIST";

    @Override
    public Class<PolicyRule.RecipientAllowlist> supportedType() {
        return PolicyRule.RecipientAllowlist.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.RecipientAllowlist rule, EvaluationContext context) {
        var recipient = context.recipientAddress().toLowerCase(Locale.ROOT);
        var allowedAddresses = rule.addresses().stream()
                .map(address -> address.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (allowedAddresses.contains(recipient)) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .message("Recipient %s is not in the allowlist".formatted(context.recipientAddress()))
                .build();
    }
}
