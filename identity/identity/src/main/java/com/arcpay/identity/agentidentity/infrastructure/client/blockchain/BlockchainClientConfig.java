package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

@Configuration
@EnableConfigurationProperties({BlockchainProperties.class, AgentRegistryProperties.class})
@ConditionalOnProperty(prefix = "arcpay.blockchain", name = "rpc-url")
class BlockchainClientConfig {

    private static final long RECEIPT_POLL_INTERVAL_MS = 1_000L;
    private static final int RECEIPT_POLL_ATTEMPTS = 40;

    @Bean
    Web3j web3j(BlockchainProperties properties) {
        return Web3j.build(new HttpService(properties.rpcUrl()));
    }

    @Bean
    Credentials platformWalletCredentials(BlockchainProperties properties) {
        return Credentials.create(properties.platformWalletPrivateKey());
    }

    @Bean
    FastRawTransactionManager agentRegistryTransactionManager(
            Web3j web3j, Credentials platformWalletCredentials, BlockchainProperties properties) {
        return new FastRawTransactionManager(web3j, platformWalletCredentials, properties.chainId());
    }

    @Bean
    TransactionReceiptProcessor transactionReceiptProcessor(Web3j web3j) {
        return new PollingTransactionReceiptProcessor(web3j, RECEIPT_POLL_INTERVAL_MS, RECEIPT_POLL_ATTEMPTS);
    }
}
