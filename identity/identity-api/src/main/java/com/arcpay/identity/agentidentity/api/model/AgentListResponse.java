package com.arcpay.identity.agentidentity.api.model;

import java.util.List;
import lombok.Builder;

@Builder
public record AgentListResponse(List<AgentResponse> content, int page, int size, long totalElements, int totalPages) {}
