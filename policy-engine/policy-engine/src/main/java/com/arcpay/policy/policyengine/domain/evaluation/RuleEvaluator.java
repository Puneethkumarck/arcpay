package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;

public interface RuleEvaluator<T extends PolicyRule> {

    Class<T> supportedType();

    RuleEvaluationResult evaluate(T rule, EvaluationContext context);
}
