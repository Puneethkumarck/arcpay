package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import java.math.BigInteger;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arcpay.contract")
record AgentRegistryProperties(
        String agentRegistryAddress, BigInteger gasLimit, BigInteger gasPrice, Long gasPriceMultiplierPercent) {

    AgentRegistryProperties {
        if (gasLimit == null) {
            gasLimit = BigInteger.valueOf(300_000L);
        }
        if (gasPrice == null) {
            gasPrice = BigInteger.valueOf(1_000_000_000L);
        }
        if (gasPriceMultiplierPercent == null) {
            gasPriceMultiplierPercent = 200L;
        }
    }
}
