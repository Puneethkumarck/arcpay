package com.arcpay.policy.policyengine.api.model;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ReservationResponse(UUID paymentId, UUID agentId, String status) {}
