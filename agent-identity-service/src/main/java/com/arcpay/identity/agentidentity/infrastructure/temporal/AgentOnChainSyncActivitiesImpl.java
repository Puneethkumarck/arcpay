package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentOnChainSyncActivities;
import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "AgentIdentityTaskQueue")
class AgentOnChainSyncActivitiesImpl implements AgentOnChainSyncActivities {

    private final BlockchainService blockchainService;

    @Override
    public void syncToChain(AgentOnChainSyncRequest request) {
        log.info("Syncing to chain agentId={} operation={}", request.agentId(), request.operation());
        switch (request.operation()) {
            case DEACTIVATE -> blockchainService.deactivateAgent(request.agentId());
            case REACTIVATE -> blockchainService.reactivateAgent(request.agentId());
            case UPDATE_METADATA -> blockchainService.updateMetadata(
                    request.agentId(), request.parameters().get("metadataHash"));
            case UPDATE_POLICY -> blockchainService.updatePolicy(
                    request.agentId(), request.parameters().get("policyHash"));
        }
        log.info("Chain sync complete agentId={} operation={}", request.agentId(), request.operation());
    }
}
