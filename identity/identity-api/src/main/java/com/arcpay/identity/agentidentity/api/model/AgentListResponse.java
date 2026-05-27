package com.arcpay.identity.agentidentity.api.model;

import lombok.Builder;

import java.util.List;

@Builder
public record AgentListResponse(
        List<AgentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
