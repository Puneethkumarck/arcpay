package com.arcpay.payment.paymentexecution.infrastructure.client.policy;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PolicyResultMapper {

    @Mapping(target = "rulesEvaluated", source = "ruleResults", qualifiedByName = "countRules")
    PolicyResult toDomain(PolicyEvaluationResponse response);

    @Named("countRules")
    default Integer countRules(List<?> ruleResults) {
        return ruleResults == null ? 0 : ruleResults.size();
    }
}
