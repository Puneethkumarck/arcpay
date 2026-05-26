package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ErrorMessages;
import com.arcpay.identity.agentidentity.api.model.validator.ValidPolicyHash;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAgentRequest(
        @NotBlank(message = ErrorMessages.AGENT_NAME_REQUIRED)
        @Size(min = 3, max = 64, message = ErrorMessages.AGENT_NAME_SIZE)
        String name,

        @NotBlank(message = ErrorMessages.PURPOSE_REQUIRED)
        @Size(max = 256, message = ErrorMessages.PURPOSE_SIZE)
        String purpose,

        @ValidPolicyHash
        String policyHash
) {}
