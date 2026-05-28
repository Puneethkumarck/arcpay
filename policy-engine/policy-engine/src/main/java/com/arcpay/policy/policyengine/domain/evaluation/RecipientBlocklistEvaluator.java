package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecipientBlocklistEvaluator implements RuleEvaluator<PolicyRule.RecipientBlocklist> {

    private static final String RULE_TYPE = "RECIPIENT_BLOCKLIST";

    @Override
    public Class<PolicyRule.RecipientBlocklist> supportedType() {
        return PolicyRule.RecipientBlocklist.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.RecipientBlocklist rule, EvaluationContext context) {
        var recipient = context.recipientAddress().toLowerCase();
        var blockedAddresses = rule.addresses().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (!blockedAddresses.contains(recipient)) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .message("Recipient %s is blocklisted".formatted(context.recipientAddress()))
                .build();
    }
}
