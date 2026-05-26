package com.arcpay.identity.agentidentity.api.model;

import jakarta.validation.constraints.Size;

public record UpdateAgentRequest(
        @Size(min = 3, max = 64, message = "Agent name must be between 3 and 64 characters")
        String name,

        @Size(max = 256, message = "Purpose must not exceed 256 characters")
        String purpose
) {}
