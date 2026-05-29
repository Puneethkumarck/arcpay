package com.arcpay.compliance.infrastructure.onchain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "compliance.onchain")
record OnChainProperties(
        String rpcUrl,
        String usdcContract,
        long scanBlockWindow,
        List<String> mixerAddresses,
        int mixerScore
) {

    OnChainProperties {
        scanBlockWindow = scanBlockWindow <= 0 ? 50000 : scanBlockWindow;
        mixerAddresses = mixerAddresses == null ? List.of() : List.copyOf(mixerAddresses);
        mixerScore = mixerScore <= 0 ? 50 : mixerScore;
    }
}
