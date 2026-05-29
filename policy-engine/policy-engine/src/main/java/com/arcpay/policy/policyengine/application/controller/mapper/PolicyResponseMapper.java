package com.arcpay.policy.policyengine.application.controller.mapper;

import com.arcpay.policy.policyengine.api.model.PolicyListResponse;
import com.arcpay.policy.policyengine.api.model.PolicyResponse;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface PolicyResponseMapper {

    PolicyResponse toApi(Policy policy);

    default String map(PolicyStatus status) {
        return status.name();
    }

    default PolicyListResponse toApi(Page<Policy> page) {
        var content = page.getContent().stream().map(this::toApi).toList();
        return PolicyListResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
