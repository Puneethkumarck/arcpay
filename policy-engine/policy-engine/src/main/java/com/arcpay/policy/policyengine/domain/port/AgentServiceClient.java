package com.arcpay.policy.policyengine.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface AgentServiceClient {

    Optional<AgentInfo> resolveApiKey(String apiKeyHash);

    Optional<AgentInfo> getAgent(UUID agentId);

    void updatePolicy(UUID agentId, String policyHash);

    record AgentInfo(
            UUID agentId,
            UUID ownerId,
            String status,
            String policyHash
    ) {}
}
