package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningActivities;
import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningWorkflow;
import com.arcpay.identity.agentidentity.domain.model.AgentProvisioningRequest;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "AgentIdentityTaskQueue")
class AgentProvisioningWorkflowImpl implements AgentProvisioningWorkflow {

    private static final Logger log = Workflow.getLogger(AgentProvisioningWorkflowImpl.class);

    private final AgentProvisioningActivities walletActivities = Workflow.newActivityStub(
            AgentProvisioningActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    private final AgentProvisioningActivities onChainActivities = Workflow.newActivityStub(
            AgentProvisioningActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(5)
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2.0)
                            .build())
                    .build());

    private final AgentProvisioningActivities compensationActivities = Workflow.newActivityStub(
            AgentProvisioningActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .build())
                    .build());

    @Override
    public void provision(AgentProvisioningRequest request) {
        log.info("Starting provisioning for agentId={}", request.agentId());
        try {
            walletActivities.createCircleWallet(request.agentId());
            onChainActivities.registerOnChain(request.agentId());
            log.info("Provisioning completed for agentId={}", request.agentId());
        } catch (ActivityFailure e) {
            var failedStep = determineFailedStep(e);
            var reason = extractReason(e);
            log.warn("Provisioning failed for agentId={} step={} reason={}", request.agentId(), failedStep, reason);
            compensationActivities.failProvisioning(request.agentId(), failedStep, reason);
        }
    }

    private String determineFailedStep(ActivityFailure e) {
        var activityType = e.getActivityType();
        if (activityType != null && activityType.contains("createCircleWallet")) {
            return "WALLET_CREATION";
        }
        return "ON_CHAIN_REGISTRATION";
    }

    private String extractReason(ActivityFailure e) {
        if (e.getCause() != null) {
            return e.getCause().getMessage();
        }
        return e.getMessage();
    }
}
