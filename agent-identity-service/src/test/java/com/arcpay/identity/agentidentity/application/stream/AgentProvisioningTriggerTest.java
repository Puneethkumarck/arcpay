package com.arcpay.identity.agentidentity.application.stream;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningWorkflow;
import com.arcpay.identity.agentidentity.domain.event.AgentRegistrationRequested;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_METADATA_HASH;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AgentProvisioningTriggerTest {

    @Mock
    private WorkflowClient workflowClient;

    @InjectMocks
    private AgentProvisioningTrigger trigger;

    @Test
    void shouldStartProvisioningWorkflowWithCorrectOptions() {
        // given
        var event = new AgentRegistrationRequested(
                SOME_AGENT_ID, SOME_OWNER_ID, "test-agent", "test purpose",
                SOME_METADATA_HASH, Instant.now());

        var expectedOptions = WorkflowOptions.newBuilder()
                .setWorkflowId("AgentProvisioning_" + SOME_AGENT_ID)
                .setTaskQueue("AgentIdentityTaskQueue")
                .setWorkflowExecutionTimeout(Duration.ofSeconds(300))
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        var mockWorkflow = mock(AgentProvisioningWorkflow.class);
        given(workflowClient.newWorkflowStub(AgentProvisioningWorkflow.class, expectedOptions))
                .willReturn(mockWorkflow);

        // when
        trigger.onAgentRegistrationRequested(event);

        // then
        then(workflowClient).should().newWorkflowStub(AgentProvisioningWorkflow.class, expectedOptions);
    }
}
