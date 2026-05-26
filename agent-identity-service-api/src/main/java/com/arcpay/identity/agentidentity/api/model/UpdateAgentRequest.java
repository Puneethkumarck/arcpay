package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ErrorMessages;
import jakarta.validation.constraints.Size;

public record UpdateAgentRequest(
        @Size(min = 3, max = 64, message = ErrorMessages.AGENT_NAME_SIZE)
        String name,

        @Size(max = 256, message = ErrorMessages.PURPOSE_SIZE)
        String purpose
) {}
