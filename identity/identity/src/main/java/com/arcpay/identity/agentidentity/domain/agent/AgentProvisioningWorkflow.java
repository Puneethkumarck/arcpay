package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentProvisioningRequest;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface AgentProvisioningWorkflow {

    @WorkflowMethod
    void provision(AgentProvisioningRequest request);

    static String workflowId(UUID agentId) {
        return "AgentProvisioning_" + agentId;
    }
}
