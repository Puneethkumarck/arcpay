package com.arcpay.settlement.infrastructure.circle;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "settlement")
record SettlementProperties(
        BigDecimal gasBufferUsdc
) {

    SettlementProperties {
        if (gasBufferUsdc == null) {
            gasBufferUsdc = BigDecimal.ZERO;
        }
    }
}
