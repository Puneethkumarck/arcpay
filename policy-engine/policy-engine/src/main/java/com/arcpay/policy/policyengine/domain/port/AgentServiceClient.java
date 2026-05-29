package com.arcpay.policy.policyengine.domain.port;

import com.arcpay.policy.policyengine.domain.model.AgentInfo;

import java.util.Optional;
import java.util.UUID;

public interface AgentServiceClient {

    Optional<AgentInfo> getAgent(UUID agentId);

    void updatePolicy(UUID agentId, String policyHash);
}
