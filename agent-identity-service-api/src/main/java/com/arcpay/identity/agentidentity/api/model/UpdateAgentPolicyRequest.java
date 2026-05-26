package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ErrorMessages;
import com.arcpay.identity.agentidentity.api.model.validator.ValidPolicyHash;
import jakarta.validation.constraints.NotBlank;

public record UpdateAgentPolicyRequest(
        @NotBlank(message = ErrorMessages.POLICY_HASH_REQUIRED)
        @ValidPolicyHash
        String policyHash
) {}
