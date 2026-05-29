package com.arcpay.compliance.fixtures;

import org.web3j.protocol.core.methods.response.EthLog;

import java.util.List;

public final class OnChainFixtures {

    public static final String TRANSFER_EVENT_TOPIC =
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

    public static final String SOME_USDC_CONTRACT = "0x0000000000000000000000000000000000000abc";

    private OnChainFixtures() {}

    public static EthLog ethLogResponse(List<EthLog.LogResult> logs) {
        var response = new EthLog();
        response.setResult(logs);
        return response;
    }

    public static EthLog.LogObject transferLog(String fromAddress, String toAddress) {
        var log = new EthLog.LogObject();
        log.setTopics(List.of(TRANSFER_EVENT_TOPIC, padTopic(fromAddress), padTopic(toAddress)));
        return log;
    }

    public static String padTopic(String address) {
        var hex = address.replaceFirst("^0x", "");
        return "0x" + "0".repeat(24) + hex;
    }

    public static String blockNumberJson(long blockNumber) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"0x"
                + Long.toHexString(blockNumber) + "\"}";
    }

    public static String emptyLogsJson() {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":[]}";
    }

    public static String transferLogsJson(String fromAddress, String toAddress) {
        return "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":[{"
                + "\"address\":\"" + SOME_USDC_CONTRACT + "\","
                + "\"blockNumber\":\"0x1\","
                + "\"transactionHash\":\"0x" + "0".repeat(64) + "\","
                + "\"data\":\"0x\","
                + "\"topics\":[\"" + TRANSFER_EVENT_TOPIC + "\",\""
                + padTopic(fromAddress) + "\",\"" + padTopic(toAddress) + "\"]"
                + "}]}";
    }
}
