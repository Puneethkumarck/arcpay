package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentOnChainSyncWorkflow;
import com.arcpay.identity.agentidentity.domain.model.AgentOnChainSyncRequest;
import com.arcpay.identity.agentidentity.domain.model.OnChainOperation;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

class AgentOnChainSyncWorkflowIntegrationTest extends FullContextIntegrationTest {

    @MockitoBean
    private BlockchainService blockchainService;

    @Autowired
    private WorkflowClient workflowClient;

    @Test
    void shouldSyncDeactivateSuccessfully() {
        // given
        given(blockchainService.deactivateAgent(SOME_AGENT_ID)).willReturn("0xtxhash");

        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.DEACTIVATE)
                .parameters(Map.of())
                .build();

        // when
        syncWorkflow(request);

        // then
        then(blockchainService).should().deactivateAgent(SOME_AGENT_ID);
    }

    @Test
    void shouldCompleteWithoutThrowingOnSyncFailure() {
        // given
        given(blockchainService.reactivateAgent(SOME_AGENT_ID))
                .willThrow(new RuntimeException("Blockchain unreachable"));

        var request = AgentOnChainSyncRequest.builder()
                .agentId(SOME_AGENT_ID)
                .operation(OnChainOperation.REACTIVATE)
                .parameters(Map.of())
                .build();

        // when — should complete without throwing even though sync failed
        syncWorkflow(request);

        // then
        then(blockchainService).should(atLeastOnce()).reactivateAgent(SOME_AGENT_ID);
    }

    private void syncWorkflow(AgentOnChainSyncRequest request) {
        var workflow = workflowClient.newWorkflowStub(
                AgentOnChainSyncWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(AgentOnChainSyncWorkflow.workflowId(request.agentId(), request.operation()))
                        .setTaskQueue("AgentIdentityTaskQueue")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                        .build());
        workflow.sync(request);
    }
}
