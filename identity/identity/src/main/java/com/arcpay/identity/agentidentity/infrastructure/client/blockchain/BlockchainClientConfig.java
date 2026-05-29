package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
@EnableConfigurationProperties(BlockchainProperties.class)
@ConditionalOnProperty(prefix = "arcpay.blockchain", name = "rpc-url")
class BlockchainClientConfig {

    @Bean
    Web3j web3j(BlockchainProperties properties) {
        return Web3j.build(new HttpService(properties.rpcUrl()));
    }

    @Bean
    String agentRegistryContractAddress(
            @Value("${arcpay.contract.agent-registry-address}") String contractAddress) {
        return contractAddress;
    }
}
