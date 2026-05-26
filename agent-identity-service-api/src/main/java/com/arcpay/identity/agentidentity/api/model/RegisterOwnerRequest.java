package com.arcpay.identity.agentidentity.api.model;

import com.arcpay.identity.agentidentity.api.model.validator.ValidWalletAddress;
import jakarta.validation.constraints.NotBlank;

public record RegisterOwnerRequest(
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Wallet address is required")
        @ValidWalletAddress
        String walletAddress
) {}
