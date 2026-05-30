package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "circle.api")
record CircleApiProperties(
        String baseUrl,
        String apiKey,
        String walletSetId,
        String blockchain,
        String entitySecret,
        int connectTimeoutMs,
        int readTimeoutMs
) {}
