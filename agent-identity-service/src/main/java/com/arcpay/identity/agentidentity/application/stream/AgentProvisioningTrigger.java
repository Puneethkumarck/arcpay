package com.arcpay.identity.agentidentity.application.stream;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningWorkflow;
import com.arcpay.identity.agentidentity.domain.event.AgentRegistrationRequested;
import com.arcpay.identity.agentidentity.domain.model.AgentProvisioningRequest;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
class AgentProvisioningTrigger {

    private final WorkflowClient workflowClient;

    @KafkaListener(topics = AgentRegistrationRequested.TOPIC)
    void onAgentRegistrationRequested(AgentRegistrationRequested event) {
        log.info("Received agent registration event agentId={}", event.agentId());
        var request = AgentProvisioningRequest.builder()
                .agentId(event.agentId())
                .ownerId(event.ownerId())
                .name(event.name())
                .purpose(event.purpose())
                .metadataHash(event.metadataHash())
                .build();

        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(AgentProvisioningWorkflow.workflowId(event.agentId()))
                .setTaskQueue("AgentIdentityTaskQueue")
                .setWorkflowExecutionTimeout(Duration.ofSeconds(300))
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();

        try {
            var workflow = workflowClient.newWorkflowStub(AgentProvisioningWorkflow.class, options);
            WorkflowClient.start(workflow::provision, request);
            log.info("Started provisioning workflow for agentId={}", event.agentId());
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.info("Provisioning workflow already running for agentId={}, skipping duplicate", event.agentId());
        }
    }
}
