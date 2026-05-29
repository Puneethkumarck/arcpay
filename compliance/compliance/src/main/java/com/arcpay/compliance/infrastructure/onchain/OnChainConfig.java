package com.arcpay.compliance.infrastructure.onchain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@EnableConfigurationProperties(OnChainProperties.class)
class OnChainConfig {

    @Bean
    @ConditionalOnProperty(prefix = "compliance.onchain", name = "rpc-url")
    Web3j complianceWeb3j(OnChainProperties properties) {
        return Web3j.build(new HttpService(properties.rpcUrl()));
    }
}
