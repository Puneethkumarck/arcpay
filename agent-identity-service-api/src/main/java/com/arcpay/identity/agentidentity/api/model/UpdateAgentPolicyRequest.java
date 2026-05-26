package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ValidPolicyHash;
import jakarta.validation.constraints.NotBlank;

public record UpdateAgentPolicyRequest(
        @NotBlank(message = "Policy hash is required")
        @ValidPolicyHash
        String policyHash
) {}
