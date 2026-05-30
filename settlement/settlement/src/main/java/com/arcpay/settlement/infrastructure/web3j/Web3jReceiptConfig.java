package com.arcpay.settlement.infrastructure.web3j;

import com.arcpay.settlement.domain.port.ReceiptWriter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({ReceiptContractProperties.class, GasWalletProperties.class})
@ConditionalOnProperty(prefix = "arcpay.gas-wallet", name = "private-key")
class Web3jReceiptConfig {

    @Bean
    Web3j receiptWeb3j(@Value("${web3j.client-address}") String clientAddress) {
        return Web3j.build(new HttpService(clientAddress));
    }

    @Bean
    Credentials gasWalletCredentials(GasWalletProperties properties) {
        return Credentials.create(properties.privateKey());
    }

    @Bean
    FastRawTransactionManager receiptTransactionManager(Web3j receiptWeb3j,
                                                        Credentials gasWalletCredentials,
                                                        ReceiptContractProperties properties) {
        return new FastRawTransactionManager(receiptWeb3j, gasWalletCredentials, properties.chainId());
    }

    @Bean
    ReceiptWriter receiptWriter(Web3j receiptWeb3j,
                                FastRawTransactionManager receiptTransactionManager,
                                ReceiptContractProperties properties,
                                MeterRegistry meterRegistry) {
        return new Web3jReceiptWriter(receiptWeb3j, receiptTransactionManager, properties, meterRegistry, Clock.systemUTC());
    }
}
