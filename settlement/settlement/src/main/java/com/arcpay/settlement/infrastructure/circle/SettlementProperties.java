package com.arcpay.settlement.infrastructure.circle;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "settlement")
record SettlementProperties(BigDecimal gasBufferUsdc) {

    SettlementProperties {
        if (gasBufferUsdc == null) {
            gasBufferUsdc = BigDecimal.ZERO;
        }
    }
}
