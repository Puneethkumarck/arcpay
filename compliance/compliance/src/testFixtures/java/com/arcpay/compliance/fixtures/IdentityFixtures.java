package com.arcpay.compliance.fixtures;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.client.IdentityServiceCallException;
import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

public final class IdentityFixtures {

    private IdentityFixtures() {}

    public static final UUID SOME_OWNER_ID = UUID.fromString("0197aa00-6666-7def-8000-666666666666");
    public static final UUID SOME_OTHER_OWNER_ID = UUID.fromString("0197aa00-7777-7def-8000-777777777777");
    public static final String SOME_OWNER_EMAIL = "owner@arcpay.io";
    public static final String SOME_OFFICER_EMAIL = "officer@arcpay.io";
    public static final String SOME_API_KEY_HASH = "abc123hash";

    public static final OwnerPrincipalResponse SOME_OWNER_PRINCIPAL_RESPONSE = OwnerPrincipalResponse.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_OWNER_EMAIL)
            .authority("OWNER")
            .build();

    public static final OwnerPrincipalResponse SOME_OFFICER_PRINCIPAL_RESPONSE = OwnerPrincipalResponse.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_OFFICER_EMAIL)
            .authority("COMPLIANCE_OFFICER")
            .build();

    public static final AgentResponse SOME_AGENT_RESPONSE = AgentResponse.builder()
            .agentId(ComplianceFixtures.SOME_AGENT_ID)
            .ownerId(SOME_OWNER_ID)
            .status(AgentStatusEnum.ACTIVE)
            .policyHash("0xabc123def456")
            .name("test-agent")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

    public static String someAgentResponseJson() {
        return """
                {
                  "agentId": "%s",
                  "ownerId": "%s",
                  "status": "ACTIVE",
                  "policyHash": "0xabc123def456",
                  "name": "test-agent",
                  "createdAt": "2026-01-01T00:00:00Z"
                }
                """.formatted(ComplianceFixtures.SOME_AGENT_ID, SOME_OWNER_ID);
    }

    public static FeignException.NotFound feignNotFound() {
        return new FeignException.NotFound("Not Found", someRequest(), null, Collections.emptyMap());
    }

    public static FeignException feignServerError() {
        return new FeignException.InternalServerError("Internal Server Error",
                someRequest(), null, Collections.emptyMap());
    }

    public static IdentityServiceCallException clientUnavailable() {
        return new IdentityServiceCallException("Identity service call failed", feignServerError());
    }

    public static IdentityServiceCallException clientCallFailedWithCause(Throwable cause) {
        return new IdentityServiceCallException("Identity service call failed", cause);
    }

    private static Request someRequest() {
        return Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }
}
