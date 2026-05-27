package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import com.arcpay.identity.agentidentity.domain.model.OnChainOperation;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface AgentOnChainSyncWorkflow {

    @WorkflowMethod
    void sync(AgentOnChainSyncRequest request);

    static String workflowId(UUID agentId, OnChainOperation operation) {
        return "AgentOnChainSync_" + agentId + "_" + operation;
    }
}
