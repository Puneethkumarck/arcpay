package com.arcpay.compliance.infrastructure.onchain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnBean(Web3j.class)
@RequiredArgsConstructor
class UsdcTransferLogScanner {

    private static final Event TRANSFER_EVENT = new Event(
            "Transfer",
            List.of(
                    TypeReference.create(Address.class, true),
                    TypeReference.create(Address.class, true),
                    TypeReference.create(Uint256.class, false)));

    private final Web3j web3j;
    private final OnChainProperties properties;

    Set<String> counterpartiesOf(String recipientAddress) {
        try {
            var latest = web3j.ethBlockNumber().send().getBlockNumber();
            var fromBlock = latest.subtract(BigInteger.valueOf(properties.scanBlockWindow()));
            if (fromBlock.signum() < 0) {
                fromBlock = BigInteger.ZERO;
            }
            var topic = paddedTopic(recipientAddress);
            var counterparties = new LinkedHashSet<String>();
            counterparties.addAll(query(fromBlock, latest, recipientAddress, topic, true));
            counterparties.addAll(query(fromBlock, latest, recipientAddress, topic, false));
            return counterparties;
        } catch (Exception ex) {
            log.warn("eth_getLogs scan failed for recipient={}: {}", recipientAddress, ex.getMessage());
            return Set.of();
        }
    }

    private Set<String> query(
            BigInteger fromBlock,
            BigInteger toBlock,
            String recipientAddress,
            String recipientTopic,
            boolean recipientIsFrom) throws Exception {
        var filter = new EthFilter(
                DefaultBlockParameter.valueOf(fromBlock),
                DefaultBlockParameter.valueOf(toBlock),
                properties.usdcContract());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        if (recipientIsFrom) {
            filter.addSingleTopic(recipientTopic);
            filter.addNullTopic();
        } else {
            filter.addNullTopic();
            filter.addSingleTopic(recipientTopic);
        }
        var ethLog = web3j.ethGetLogs(filter).send();
        var counterparties = new LinkedHashSet<String>();
        for (var logResult : ethLog.getLogs()) {
            var topics = ((EthLog.LogObject) logResult.get()).getTopics();
            if (topics.size() < 3) {
                continue;
            }
            var fromAddress = addressOf(topics.get(1));
            var toAddress = addressOf(topics.get(2));
            var counterparty = recipientIsFrom ? toAddress : fromAddress;
            if (!counterparty.equals(recipientAddress)) {
                counterparties.add(counterparty);
            }
        }
        return counterparties;
    }

    private static String paddedTopic(String address) {
        var hex = address.toLowerCase(Locale.ROOT).replaceFirst("^0x", "");
        return "0x" + "0".repeat(24) + hex;
    }

    private static String addressOf(String topic) {
        var hex = topic.toLowerCase(Locale.ROOT).replaceFirst("^0x", "");
        return "0x" + hex.substring(hex.length() - 40);
    }
}
