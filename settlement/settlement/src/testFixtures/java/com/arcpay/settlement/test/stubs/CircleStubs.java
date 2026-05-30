package com.arcpay.settlement.test.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class CircleStubs {

    private CircleStubs() {}

    public static void stubWalletBalance(WireMockServer circleServer, String walletId, String amount) {
        circleServer.stubFor(get(urlPathEqualTo("/v1/w3s/wallets/" + walletId + "/balances"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "tokenBalances": [
                                      { "amount": "%s" }
                                    ]
                                  }
                                }
                                """.formatted(amount))));
    }

    public static void stubCreateTransfer(WireMockServer circleServer, String circleTxId, String state) {
        circleServer.stubFor(post(urlPathEqualTo("/v1/w3s/developer/transactions/transfer"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "id": "%s",
                                    "state": "%s"
                                  }
                                }
                                """.formatted(circleTxId, state))));
    }

    public static void stubNotificationPublicKey(WireMockServer circleServer, String keyId, String publicKey) {
        circleServer.stubFor(get(urlPathMatching("/v2/notifications/publicKey/" + keyId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "id": "%s",
                                    "publicKey": "%s"
                                  }
                                }
                                """.formatted(keyId, publicKey))));
    }
}
