package com.arcpay.settlement.infrastructure.circle;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "circle.api")
record CircleApiProperties(
        String baseUrl,
        String apiKey,
        String walletSetId,
        String blockchain,
        String usdcTokenAddress,
        String entitySecret,
        Timeout timeout
) {

    record Timeout(int connect, int read) {}
}
