package com.arcpay.identity.agentidentity.domain.exception;

public class OwnerWalletAlreadyExistsException extends RuntimeException {

    public OwnerWalletAlreadyExistsException(String walletAddress) {
        super("Owner with wallet address '" + walletAddress + "' already exists");
    }
}
