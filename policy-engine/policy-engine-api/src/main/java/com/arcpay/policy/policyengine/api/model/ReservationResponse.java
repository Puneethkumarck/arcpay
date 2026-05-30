package com.arcpay.policy.policyengine.api.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ReservationResponse(
        UUID paymentId,
        UUID agentId,
        String status
) {}
