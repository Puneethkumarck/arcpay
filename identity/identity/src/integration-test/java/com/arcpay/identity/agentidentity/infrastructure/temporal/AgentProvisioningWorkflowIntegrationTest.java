package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningWorkflow;
import com.arcpay.identity.agentidentity.domain.model.AgentProvisioningRequest;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.model.WalletCreationResult;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_METADATA_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_PROVISIONING_REQUEST;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_TX_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ID;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        cleanDatabase();
        jdbcTemplate.update("""
                INSERT INTO owners (owner_id, email, wallet_address, api_key_hash, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                ON CONFLICT DO NOTHING""",
                SOME_OWNER_ID, "test@example.com", "0xwallet", "hash");
        agentRepository.save(SOME_AGENT_PROVISIONING);
    }

    @AfterEach
    void cleanUp() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM agents");
        jdbcTemplate.update("DELETE FROM owners");
    }

    @Test
    void shouldCompleteFullProvisioningSequence() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .willReturn(new RegistrationResult(SOME_TX_HASH, 42L));

        // when
        provisionWorkflow(SOME_PROVISIONING_REQUEST);

        // then
        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        var expected = SOME_AGENT_PROVISIONING.toBuilder()
                .status(AgentStatus.ACTIVE)
                .walletId(SOME_WALLET_ID)
                .walletAddress(SOME_WALLET_ADDRESS)
                .onChainTxHash(SOME_TX_HASH)
                .build();
        assertThat(agent)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(expected);
    }

    @Test
    void shouldFailProvisioningOnWalletCreationFailure() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willThrow(new RuntimeException("Circle API unavailable"));

        // when / then
        assertThatThrownBy(() -> provisionWorkflow(SOME_PROVISIONING_REQUEST))
                .isInstanceOf(WorkflowFailedException.class);

        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        var expected = SOME_AGENT_PROVISIONING.toBuilder()
                .status(AgentStatus.FAILED)
                .build();
        assertThat(agent)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .ignoringFields("failureReason")
                .isEqualTo(expected);
        assertThat(agent.failureReason()).isNotBlank();
    }

    @Test
    void shouldFailProvisioningOnOnChainRegistrationFailure() {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .willThrow(new RuntimeException("Gas estimation failed"));

        // when / then
        assertThatThrownBy(() -> provisionWorkflow(SOME_PROVISIONING_REQUEST))
                .isInstanceOf(WorkflowFailedException.class);

        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        var expected = SOME_AGENT_PROVISIONING.toBuilder()
                .status(AgentStatus.FAILED)
                .walletId(SOME_WALLET_ID)
                .walletAddress(SOME_WALLET_ADDRESS)
                .build();
        assertThat(agent)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .ignoringFields("failureReason")
                .isEqualTo(expected);
        assertThat(agent.failureReason()).isNotBlank();
    }

    private void provisionWorkflow(AgentProvisioningRequest request) {
        var workflow = workflowClient.newWorkflowStub(
                AgentProvisioningWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(AgentProvisioningWorkflow.workflowId(request.agentId()))
                        .setTaskQueue("AgentIdentityTaskQueue")
                        .setWorkflowExecutionTimeout(Duration.ofSeconds(30))
                        .build());
        workflow.provision(request);
    }
}
