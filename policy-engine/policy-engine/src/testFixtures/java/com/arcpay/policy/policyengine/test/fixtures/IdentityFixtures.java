package com.arcpay.policy.policyengine.test.fixtures;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.client.IdentityServiceUnavailableException;
import feign.FeignException;
import feign.Request;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

public final class IdentityFixtures {

    private IdentityFixtures() {}

    public static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    public static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000003");
    public static final String SOME_POLICY_HASH = "0xabc123def456";
    public static final String SOME_API_KEY_HASH = "abc123hash";
    public static final String SOME_EMAIL = "owner@example.com";

    public static final AgentResponse SOME_AGENT_RESPONSE = AgentResponse.builder()
            .agentId(SOME_AGENT_ID)
            .ownerId(SOME_OWNER_ID)
            .status(AgentStatusEnum.ACTIVE)
            .policyHash(SOME_POLICY_HASH)
            .name("test-agent")
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

    public static final OwnerPrincipalResponse SOME_OWNER_PRINCIPAL_RESPONSE = OwnerPrincipalResponse.builder()
            .ownerId(SOME_OWNER_ID)
            .email(SOME_EMAIL)
            .build();

    public static FeignException.NotFound feignNotFound() {
        return new FeignException.NotFound("Not Found", someRequest(), null, Collections.emptyMap());
    }

    public static FeignException feignServerError() {
        return new FeignException.InternalServerError("Internal Server Error",
                someRequest(), null, Collections.emptyMap());
    }

    public static IdentityServiceUnavailableException clientUnavailable() {
        return new IdentityServiceUnavailableException("Identity service call failed", feignServerError());
    }

    private static Request someRequest() {
        return Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }
}
