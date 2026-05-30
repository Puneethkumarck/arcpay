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

    public static void stubEmptyWalletBalance(WireMockServer circleServer, String walletId) {
        circleServer.stubFor(get(urlPathEqualTo("/v1/w3s/wallets/" + walletId + "/balances"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "tokenBalances": []
                                  }
                                }
                                """)));
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

    public static void stubEntityPublicKey(WireMockServer circleServer, String pemPublicKey) {
        circleServer.stubFor(get(urlPathEqualTo("/v1/w3s/config/entity/publicKey"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "publicKey": "%s"
                                  }
                                }
                                """.formatted(pemPublicKey.replace("\n", "\\n")))));
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

    public static void stubListSubscriptions(WireMockServer circleServer, String endpoint) {
        var subscriptions = endpoint == null ? "" : """
                { "id": "sub-1", "endpoint": "%s" }
                """.formatted(endpoint);
        circleServer.stubFor(get(urlPathEqualTo("/v2/notifications/subscriptions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "subscriptions": [ %s ]
                                  }
                                }
                                """.formatted(subscriptions))));
    }

    public static void stubCreateSubscription(WireMockServer circleServer) {
        circleServer.stubFor(post(urlPathEqualTo("/v2/notifications/subscriptions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "id": "sub-created",
                                    "endpoint": "https://settlement.arcpay.dev/api/v1/webhooks/circle"
                                  }
                                }
                                """)));
    }
}
