package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ValidPolicyHash;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAgentRequest(
        @NotBlank(message = "Agent name is required")
        @Size(min = 3, max = 64, message = "Agent name must be between 3 and 64 characters")
        String name,

        @NotBlank(message = "Purpose is required")
        @Size(max = 256, message = "Purpose must not exceed 256 characters")
        String purpose,

        @ValidPolicyHash
        String policyHash
) {}
