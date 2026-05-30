package com.arcpay.payment.paymentexecution.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class SettlementServiceStubs {

    public static final String TRANSFERS_PATH = "/api/v1/internal/transfers";
    public static final String RECEIPTS_PATH = "/api/v1/internal/receipts";

    private SettlementServiceStubs() {}

    public static String balancePath(UUID agentId) {
        return "/api/v1/internal/wallets/" + agentId + "/balance";
    }

    public static void stubTransferAccepted(WireMockServer server, UUID paymentId) {
        server.stubFor(post(urlPathEqualTo(TRANSFERS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(transferJson(paymentId))));
    }

    public static void stubTransferServerError(WireMockServer server) {
        server.stubFor(post(urlPathEqualTo(TRANSFERS_PATH))
                .willReturn(aResponse().withStatus(500)));
    }

    public static void stubBalance(WireMockServer server, UUID agentId, String amount) {
        server.stubFor(get(urlPathEqualTo(balancePath(agentId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(balanceJson(agentId, amount))));
    }

    public static String transferJson(UUID paymentId) {
        return "{"
                + "\"paymentId\":\"" + paymentId + "\","
                + "\"circleTxId\":\"ctx-abc-123\","
                + "\"state\":\"PENDING\""
                + "}";
    }

    public static String balanceJson(UUID agentId, String amount) {
        return "{"
                + "\"agentId\":\"" + agentId + "\","
                + "\"walletId\":\"wallet-1\","
                + "\"amount\":" + amount + ","
                + "\"currency\":\"USDC\""
                + "}";
    }
}
