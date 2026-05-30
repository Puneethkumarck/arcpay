package com.arcpay.settlement.infrastructure.web3j;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigInteger;

@ConfigurationProperties(prefix = "arcpay.contract")
record ReceiptContractProperties(
        String paymentReceiptsAddress,
        long chainId,
        BigInteger gasLimit,
        BigInteger gasPrice,
        BigInteger lowBalanceThresholdWei
) {

    private static final long DEFAULT_CHAIN_ID = 999L;
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(150_000L);
    private static final BigInteger DEFAULT_GAS_PRICE = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger DEFAULT_LOW_BALANCE_THRESHOLD_WEI = BigInteger.valueOf(10_000_000_000_000_000L);

    ReceiptContractProperties {
        if (chainId == 0L) {
            chainId = DEFAULT_CHAIN_ID;
        }
        if (gasLimit == null) {
            gasLimit = DEFAULT_GAS_LIMIT;
        }
        if (gasPrice == null) {
            gasPrice = DEFAULT_GAS_PRICE;
        }
        if (lowBalanceThresholdWei == null) {
            lowBalanceThresholdWei = DEFAULT_LOW_BALANCE_THRESHOLD_WEI;
        }
    }
}
