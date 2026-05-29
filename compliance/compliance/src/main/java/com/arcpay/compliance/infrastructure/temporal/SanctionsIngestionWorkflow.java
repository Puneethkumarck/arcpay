package com.arcpay.compliance.infrastructure.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SanctionsIngestionWorkflow {

    String WORKFLOW_ID = "SanctionsIngestion";

    String TASK_QUEUE = "ComplianceTaskQueue";

    @WorkflowMethod
    void runIngestion(String triggerTimestamp);
}
