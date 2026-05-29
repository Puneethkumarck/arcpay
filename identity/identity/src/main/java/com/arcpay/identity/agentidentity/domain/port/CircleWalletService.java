package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.WalletCreationResult;

import java.util.UUID;

public interface CircleWalletService {

    WalletCreationResult createWallet(UUID agentId);
}
