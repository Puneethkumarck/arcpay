package com.arcpay.identity.agentidentity.domain.port;

import java.util.UUID;

public interface CircleWalletService {

    WalletCreationResult createWallet(UUID agentId);

    record WalletCreationResult(String walletId, String walletAddress) {}
}
