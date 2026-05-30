package com.arcpay.payment.paymentexecution.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class IdentityServiceStubs {

    private IdentityServiceStubs() {}

    public static String agentPath(UUID agentId) {
        return "/api/v1/internal/agents/" + agentId;
    }

    public static String apiKeyPath(String apiKeyHash) {
        return "/api/v1/internal/owners/by-api-key-hash/" + apiKeyHash;
    }

    public static void stubAgent(
            WireMockServer server, UUID agentId, UUID ownerId, String status, String walletId, String walletAddress) {
        server.stubFor(get(urlPathEqualTo(agentPath(agentId)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(agentJson(agentId, ownerId, status, walletId, walletAddress))));
    }

    public static void stubApiKey(WireMockServer server, String apiKeyHash, UUID ownerId, String email) {
        server.stubFor(get(urlPathEqualTo(apiKeyPath(apiKeyHash)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ownerPrincipalJson(ownerId, email))));
    }

    public static String agentJson(UUID agentId, UUID ownerId, String status, String walletId, String walletAddress) {
        return "{"
                + "\"agentId\":\"" + agentId + "\","
                + "\"ownerId\":\"" + ownerId + "\","
                + "\"name\":\"research-bot\","
                + "\"status\":\"" + status + "\","
                + "\"walletId\":\"" + walletId + "\","
                + "\"walletAddress\":\"" + walletAddress + "\""
                + "}";
    }

    public static String ownerPrincipalJson(UUID ownerId, String email) {
        return "{"
                + "\"ownerId\":\"" + ownerId + "\","
                + "\"email\":\"" + email + "\","
                + "\"authority\":\"OWNER\""
                + "}";
    }
}
