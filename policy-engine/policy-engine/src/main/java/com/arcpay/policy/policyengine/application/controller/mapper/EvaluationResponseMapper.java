package com.arcpay.policy.policyengine.application.controller.mapper;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.PolicyVerdict;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleVerdict;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvaluationResponseMapper {

    PolicyEvaluationResponse toApi(PolicyEvaluationResult result);

    RuleResultResponse toApi(RuleEvaluationResult result);

    default String map(PolicyVerdict verdict) {
        return verdict.name();
    }

    default String map(RuleVerdict verdict) {
        return verdict.name();
    }
}
