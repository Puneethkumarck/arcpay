package com.arcpay.identity.agentidentity.application.stream;

import com.arcpay.identity.agentidentity.domain.event.AgentRegistrationRequested;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService.WalletCreationResult;
import com.arcpay.identity.agentidentity.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ID;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_PROVISIONING;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_METADATA_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_TX_HASH;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_WALLET_ID;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class AgentProvisioningTriggerIntegrationTest extends FullContextIntegrationTest {

    @MockitoBean
    private CircleWalletService circleWalletService;

    @MockitoBean
    private BlockchainService blockchainService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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

    @SuppressWarnings("BusyWait")
    private void pollUntilAgentActive() throws InterruptedException {
        var deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
            if (agent.status() == AgentStatus.ACTIVE) {
                assertThat(agent.walletId()).isEqualTo(SOME_WALLET_ID);
                assertThat(agent.onChainTxHash()).isEqualTo(SOME_TX_HASH);
                return;
            }
            Thread.sleep(500);
        }
        var agent = agentRepository.findById(SOME_AGENT_ID).orElseThrow();
        assertThat(agent.status()).as("Agent should be ACTIVE within 30s").isEqualTo(AgentStatus.ACTIVE);
    }

    @Test
    void shouldTriggerWorkflowOnKafkaEvent() throws InterruptedException {
        // given
        given(circleWalletService.createWallet(SOME_AGENT_ID))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .willReturn(new RegistrationResult(SOME_TX_HASH, 42L));

        var event = new AgentRegistrationRequested(
                SOME_AGENT_ID, SOME_OWNER_ID, "shopping-agent-01",
                "Automated USDC payments", SOME_METADATA_HASH, Instant.now());

        // when
        kafkaTemplate.send(AgentRegistrationRequested.TOPIC, SOME_AGENT_ID.toString(), event);

        // then — poll until the async workflow completes
        pollUntilAgentActive();
    }
}
