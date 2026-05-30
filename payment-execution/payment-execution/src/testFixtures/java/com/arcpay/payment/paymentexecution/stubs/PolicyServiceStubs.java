package com.arcpay.payment.paymentexecution.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class PolicyServiceStubs {

    public static final String RESERVE_PATH = "/api/v1/internal/policies/reservations";

    private PolicyServiceStubs() {}

    public static String commitPath(UUID paymentId) {
        return RESERVE_PATH + "/" + paymentId + "/commit";
    }

    public static String releasePath(UUID paymentId) {
        return RESERVE_PATH + "/" + paymentId + "/release";
    }

    public static void stubReserveApproved(WireMockServer server) {
        server.stubFor(post(urlPathEqualTo(RESERVE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(reserveApprovedJson())));
    }

    public static void stubReserveServerError(WireMockServer server) {
        server.stubFor(post(urlPathEqualTo(RESERVE_PATH))
                .willReturn(aResponse().withStatus(500)));
    }

    public static void stubReserveClientError(WireMockServer server) {
        server.stubFor(post(urlPathEqualTo(RESERVE_PATH))
                .willReturn(aResponse().withStatus(422)));
    }

    public static String reserveApprovedJson() {
        return "{"
                + "\"verdict\":\"APPROVED\","
                + "\"dryRun\":false,"
                + "\"durationMs\":5,"
                + "\"ruleResults\":[{\"ruleType\":\"AMOUNT_LIMIT\",\"verdict\":\"APPROVED\"}]"
                + "}";
    }
}
