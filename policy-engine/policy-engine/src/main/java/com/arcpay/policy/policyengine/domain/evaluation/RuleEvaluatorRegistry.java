package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.exception.EvaluatorRegistrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RuleEvaluatorRegistry {

    private final Map<Class<? extends PolicyRule>, RuleEvaluator<?>> evaluators;

    public RuleEvaluatorRegistry(List<RuleEvaluator<?>> evaluatorBeans) {
        this.evaluators = index(evaluatorBeans);
        verifyAllRuleTypesCovered();
        log.info("Registered {} rule evaluators: {}", evaluators.size(), evaluators.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T extends PolicyRule> RuleEvaluator<T> getEvaluator(Class<T> ruleType) {
        var evaluator = evaluators.get(ruleType);
        if (evaluator == null) {
            throw new EvaluatorRegistrationException(
                    "No evaluator registered for rule type " + ruleType.getName());
        }
        return (RuleEvaluator<T>) evaluator;
    }

    private static Map<Class<? extends PolicyRule>, RuleEvaluator<?>> index(List<RuleEvaluator<?>> beans) {
        var map = new HashMap<Class<? extends PolicyRule>, RuleEvaluator<?>>();
        for (var evaluator : beans) {
            var type = evaluator.supportedType();
            var existing = map.putIfAbsent(type, evaluator);
            if (existing != null) {
                throw new EvaluatorRegistrationException(
                        "Multiple evaluators registered for rule type " + type.getName() + ": "
                                + existing.getClass().getName() + " and " + evaluator.getClass().getName());
            }
        }
        return Map.copyOf(map);
    }

    private void verifyAllRuleTypesCovered() {
        for (var ruleType : PolicyRule.class.getPermittedSubclasses()) {
            if (!evaluators.containsKey(ruleType)) {
                throw new EvaluatorRegistrationException(
                        "No evaluator registered for rule type " + ruleType.getName());
            }
        }
    }
}
