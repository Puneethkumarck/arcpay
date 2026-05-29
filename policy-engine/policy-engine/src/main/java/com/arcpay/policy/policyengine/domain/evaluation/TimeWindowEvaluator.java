package com.arcpay.policy.policyengine.domain.evaluation;

import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.model.EvaluationContext;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class TimeWindowEvaluator implements RuleEvaluator<PolicyRule.TimeWindow> {

    private static final String RULE_TYPE = "TIME_WINDOW";

    @Override
    public Class<PolicyRule.TimeWindow> supportedType() {
        return PolicyRule.TimeWindow.class;
    }

    @Override
    public RuleEvaluationResult evaluate(PolicyRule.TimeWindow rule, EvaluationContext context) {
        var utcTime = ZonedDateTime.ofInstant(context.requestedAt(), ZoneOffset.UTC);
        var hour = utcTime.getHour();
        var day = utcTime.getDayOfWeek();

        var withinHours = hour >= rule.startHour() && hour < rule.endHour();
        var allowedDay = rule.daysOfWeek().contains(day);

        if (withinHours && allowedDay) {
            return RuleEvaluationResult.builder()
                    .ruleType(RULE_TYPE)
                    .verdict(RuleVerdict.PASS)
                    .build();
        }

        return RuleEvaluationResult.builder()
                .ruleType(RULE_TYPE)
                .verdict(RuleVerdict.FAIL)
                .message("Transaction outside allowed time window (%d-%d UTC, %s)".formatted(
                        rule.startHour(), rule.endHour(), rule.daysOfWeek()))
                .build();
    }
}
