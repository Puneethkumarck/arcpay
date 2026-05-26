package com.arcpay.identity.agentidentity.domain.exception;

public class InvalidWalletAddressException extends RuntimeException {

    public InvalidWalletAddressException(String walletAddress) {
        super("Invalid wallet address: '" + walletAddress + "'");
    }
}
