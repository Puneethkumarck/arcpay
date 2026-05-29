package com.arcpay.policy.policyengine.application.controller.internal.mapper;

import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.RuleResultResponse;
import com.arcpay.policy.policyengine.domain.model.PolicyEvaluationResult;
import com.arcpay.policy.policyengine.domain.model.RuleEvaluationResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EvaluationResponseMapper {

    @Mapping(target = "verdict", expression = "java(result.verdict().name())")
    PolicyEvaluationResponse toApi(PolicyEvaluationResult result);

    @Mapping(target = "verdict", expression = "java(ruleResult.verdict().name())")
    RuleResultResponse toApi(RuleEvaluationResult ruleResult);
}
