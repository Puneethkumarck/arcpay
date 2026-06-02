package com.arcpay.policy.policyengine.api.model;

import java.util.List;
import lombok.Builder;

@Builder
public record PolicyListResponse(
        List<PolicyResponse> content, int page, int size, long totalElements, int totalPages) {}
