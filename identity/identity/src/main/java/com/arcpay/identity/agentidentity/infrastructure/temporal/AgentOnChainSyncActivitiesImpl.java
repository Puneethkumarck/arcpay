package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentOnChainSyncActivities;
import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import io.temporal.failure.ApplicationFailure;
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
                    request.agentId(), requireParameter(request, "metadataHash"));
            case UPDATE_POLICY -> blockchainService.updatePolicy(
                    request.agentId(), requireParameter(request, "policyHash"));
        }
        log.info("Chain sync complete agentId={} operation={}", request.agentId(), request.operation());
    }

    private String requireParameter(AgentOnChainSyncRequest request, String key) {
        var value = request.parameters().get(key);
        if (value == null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Missing required parameter '%s' for operation %s on agent %s"
                            .formatted(key, request.operation(), request.agentId()),
                    "MISSING_PARAMETER");
        }
        return value;
    }
}
