package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentOnChainSyncActivities;
import com.arcpay.identity.agentidentity.domain.agent.AgentOnChainSyncWorkflow;
import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "AgentIdentityTaskQueue")
class AgentOnChainSyncWorkflowImpl implements AgentOnChainSyncWorkflow {

    private static final Logger log = Workflow.getLogger(AgentOnChainSyncWorkflowImpl.class);

    private final AgentOnChainSyncActivities syncActivities = Workflow.newActivityStub(
            AgentOnChainSyncActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(10)
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(300))
                            .build())
                    .build());

    @Override
    public void sync(AgentOnChainSyncRequest request) {
        log.info("Starting on-chain sync for agentId={} operation={}", request.agentId(), request.operation());
        try {
            syncActivities.syncToChain(request);
            log.info("On-chain sync completed for agentId={} operation={}", request.agentId(), request.operation());
        } catch (Exception e) {
            log.warn("On-chain sync failed for agentId={} operation={}: {}",
                    request.agentId(), request.operation(), e.getMessage());
        }
    }
}
