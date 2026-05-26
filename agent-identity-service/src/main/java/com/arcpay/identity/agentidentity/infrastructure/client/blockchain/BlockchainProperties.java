package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arcpay.blockchain")
record BlockchainProperties(
        String rpcUrl,
        long chainId,
        String platformWalletPrivateKey
) {}
