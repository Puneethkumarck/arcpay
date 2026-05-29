package com.arcpay.compliance.test.stubs;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class IdentityStubs {

    private IdentityStubs() {}

    public static void stubResolveApiKey(WireMockServer identityServer, String hash) {
        identityServer.stubFor(get(urlPathEqualTo("/api/v1/internal/owners/by-api-key-hash/" + hash))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "ownerId": "%s",
                                  "email": "%s",
                                  "authority": "OWNER"
                                }
                                """.formatted(SOME_OWNER_ID, SOME_OWNER_EMAIL))));
    }
}
