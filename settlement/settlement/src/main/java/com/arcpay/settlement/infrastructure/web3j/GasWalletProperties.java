package com.arcpay.settlement.infrastructure.web3j;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arcpay.gas-wallet")
record GasWalletProperties(
        String privateKey
) {}
