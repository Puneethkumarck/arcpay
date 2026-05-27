package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AgentCreationService {

    public Agent createAgent(UUID ownerId, String name, String purpose, String policyHash) {
        var agentId = UuidCreator.getTimeOrderedEpoch();
        var metadataHash = MetadataHashUtil.computeMetadataHash(name, purpose);
        var now = Instant.now();

        return Agent.builder()
                .agentId(agentId)
                .ownerId(ownerId)
                .name(name)
                .purpose(purpose)
                .status(AgentStatus.PROVISIONING)
                .policyHash(policyHash)
                .metadataHash(metadataHash)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
