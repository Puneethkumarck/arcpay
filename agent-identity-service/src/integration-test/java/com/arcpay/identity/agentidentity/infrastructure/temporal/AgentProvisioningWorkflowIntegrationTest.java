package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningWorkflow;
import com.arcpay.identity.agentidentity.domain.model.AgentProvisioningRequest;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService.WalletCreationResult;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_METADATA_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_TX_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ID;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class AgentProvisioningWorkflowIntegrationTest extends FullContextIntegrationTest {

    @MockitoBean
    private CircleWalletService circleWalletService;

    @MockitoBean
    private BlockchainService blockchainService;

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("""
                INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                ON CONFLICT DO NOTHING""",
                SOME_OWNER_ID, "test@example.com", "0xwallet", "hash");
        agentRepository.save(SOME_AGENT_PROVISIONING);
    }

    @Test
    void shouldCompleteFullProvisioningSequence() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .willReturn(new RegistrationResult(SOME_TX_HASH, 42L));

        var request = AgentProvisioningRequest.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .name("shopping-agent-01")
                .purpose("Automated USDC payments for e-commerce purchases")
                .metadataHash(SOME_METADATA_HASH)
                .build();

        // when
        var workflow = workflowClient.newWorkflowStub(
                AgentProvisioningWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(AgentProvisioningWorkflow.workflowId(SOME_AGENT_ID))
                        .setTaskQueue("AgentIdentityTaskQueue")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                        .build());
        workflow.provision(request);

        // then
        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        assertThat(agent.status()).isEqualTo(AgentStatus.ACTIVE);
        assertThat(agent.walletId()).isEqualTo(SOME_WALLET_ID);
        assertThat(agent.walletAddress()).isEqualTo(SOME_WALLET_ADDRESS);
        assertThat(agent.onChainTxHash()).isEqualTo(SOME_TX_HASH);
    }

    @Test
    void shouldFailProvisioningOnWalletCreationFailure() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willThrow(new RuntimeException("Circle API unavailable"));

        var request = AgentProvisioningRequest.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .name("shopping-agent-01")
                .purpose("Automated USDC payments for e-commerce purchases")
                .metadataHash(SOME_METADATA_HASH)
                .build();

        // when
        var workflow = workflowClient.newWorkflowStub(
                AgentProvisioningWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(AgentProvisioningWorkflow.workflowId(SOME_AGENT_ID))
                        .setTaskQueue("AgentIdentityTaskQueue")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                        .build());
        workflow.provision(request);

        // then
        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        assertThat(agent.status()).isEqualTo(AgentStatus.FAILED);
        assertThat(agent.failureReason()).isNotBlank();
    }

    @Test
    void shouldFailProvisioningOnOnChainRegistrationFailure() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .willThrow(new RuntimeException("Gas estimation failed"));

        var request = AgentProvisioningRequest.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .name("shopping-agent-01")
                .purpose("Automated USDC payments for e-commerce purchases")
                .metadataHash(SOME_METADATA_HASH)
                .build();

        // when
        var workflow = workflowClient.newWorkflowStub(
                AgentProvisioningWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(AgentProvisioningWorkflow.workflowId(SOME_AGENT_ID))
                        .setTaskQueue("AgentIdentityTaskQueue")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                        .build());
        workflow.provision(request);

        // then
        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        assertThat(agent.status()).isEqualTo(AgentStatus.FAILED);
        assertThat(agent.failureReason()).isNotBlank();
    }
}
