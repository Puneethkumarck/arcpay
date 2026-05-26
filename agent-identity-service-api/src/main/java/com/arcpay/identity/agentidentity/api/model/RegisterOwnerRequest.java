package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ErrorMessages;
import com.arcpay.identity.agentidentity.api.model.validator.ValidWalletAddress;
import jakarta.validation.constraints.NotBlank;

public record RegisterOwnerRequest(
        @NotBlank(message = ErrorMessages.EMAIL_REQUIRED)
        String email,

        @NotBlank(message = ErrorMessages.WALLET_ADDRESS_REQUIRED)
        @ValidWalletAddress
        String walletAddress
) {}
