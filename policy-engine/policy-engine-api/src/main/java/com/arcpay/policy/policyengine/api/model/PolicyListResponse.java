package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PolicyListResponse(
        List<PolicyResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
