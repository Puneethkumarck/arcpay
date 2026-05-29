package com.arcpay.identity.client;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

@FeignClient(
        name = "identity-service",
        url = "${arcpay.identity-service.url}",
        fallbackFactory = IdentityClientFallbackFactory.class)
public interface IdentityServiceClient {

    @GetMapping("/api/v1/internal/owners/by-api-key-hash/{hash}")
    Optional<OwnerPrincipalResponse> resolveApiKey(@PathVariable String hash);

    @GetMapping("/api/v1/internal/agents/{agentId}")
    AgentResponse getAgent(@PathVariable UUID agentId);

    @PutMapping("/api/v1/internal/agents/{agentId}/policy")
    AgentResponse updatePolicy(@PathVariable UUID agentId,
                               @RequestBody UpdateAgentPolicyRequest request);
}
