package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;

import java.util.UUID;

public interface BlockchainService {

    RegistrationResult registerAgent(UUID agentId, UUID ownerId, String metadataHash);

    String deactivateAgent(UUID agentId);

    String reactivateAgent(UUID agentId);

    String updateMetadata(UUID agentId, String metadataHash);

    String updatePolicy(UUID agentId, String policyHash);

    boolean isAgentActive(UUID agentId);
}
