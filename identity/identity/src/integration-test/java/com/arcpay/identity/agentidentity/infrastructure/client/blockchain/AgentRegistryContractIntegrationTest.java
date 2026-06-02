package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

@Testcontainers
class AgentRegistryContractIntegrationTest {

    private static final long CHAIN_ID = 1337L;
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(2_000_000_000L);
    private static final BigInteger DEPLOY_GAS_LIMIT = BigInteger.valueOf(6_000_000L);

    // ganache deterministic accounts (mnemonic seeded by --wallet.deterministic)
    private static final String REGISTRAR_KEY = "0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d";
    private static final String OUTSIDER_KEY = "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1";

    private static final String METADATA_HASH = "0xabababababababababababababababababababababababababababababababab";
    private static final String NEW_METADATA_HASH =
            "0x1111111111111111111111111111111111111111111111111111111111111111";
    private static final String POLICY_HASH = "0xcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd";

    @Container
    private static final GenericContainer<?> GANACHE = new GenericContainer<>("trufflesuite/ganache:v7.9.1")
            .withExposedPorts(8545)
            .withCommand(
                    "--wallet.deterministic",
                    "--chain.chainId",
                    Long.toString(CHAIN_ID),
                    "--miner.blockGasLimit",
                    "30000000")
            .waitingFor(Wait.forListeningPort());

    private static Web3j web3j;
    private static TransactionReceiptProcessor receiptProcessor;
    private static String contractAddress;

    @BeforeAll
    static void deployContract() throws Exception {
        web3j = Web3j.build(new HttpService("http://" + GANACHE.getHost() + ":" + GANACHE.getMappedPort(8545)));
        receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 200L, 60);

        var bytecode = Files.readString(Path.of("contracts/AgentRegistry.bin")).trim();
        var registrarManager = new FastRawTransactionManager(web3j, Credentials.create(REGISTRAR_KEY), CHAIN_ID);
        var deploy = registrarManager.sendTransaction(GAS_PRICE, DEPLOY_GAS_LIMIT, "", bytecode, BigInteger.ZERO);
        var receipt = receiptProcessor.waitForTransactionReceipt(deploy.getTransactionHash());
        contractAddress = receipt.getContractAddress();
    }

    private BlockchainAdapter adapterFor(String privateKey) {
        var manager = new FastRawTransactionManager(web3j, Credentials.create(privateKey), CHAIN_ID);
        var properties = new AgentRegistryProperties(contractAddress, null, GAS_PRICE);
        return new BlockchainAdapter(
                web3j, manager, receiptProcessor, mock(GasUsageRepository.class), properties, Clock.systemUTC());
    }

    @Test
    void shouldRoundTripAgentLifecycleOnChain() {
        // given
        var adapter = adapterFor(REGISTRAR_KEY);
        var agentId = UuidCreator.getTimeOrderedEpoch();
        var ownerId = UuidCreator.getTimeOrderedEpoch();

        // when registered
        var registration = adapter.registerAgent(agentId, ownerId, METADATA_HASH);

        // then the on-chain record reflects an active agent with real tx coordinates
        assertThat(registration.txHash()).startsWith("0x").hasSize(66);
        assertThat(registration.blockNumber()).isPositive();
        assertThat(adapter.isAgentActive(agentId)).isTrue();
        assertThat(adapter.getAgent(agentId))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new OnChainAgentView(ownerId, METADATA_HASH, zeroHash(), true, 0L));

        // when deactivated then reactivated
        adapter.deactivateAgent(agentId);
        assertThat(adapter.isAgentActive(agentId)).isFalse();
        adapter.reactivateAgent(agentId);
        assertThat(adapter.isAgentActive(agentId)).isTrue();

        // when policy and metadata are updated
        adapter.updatePolicy(agentId, POLICY_HASH);
        adapter.updateMetadata(agentId, NEW_METADATA_HASH);

        // then getAgent reflects every change
        assertThat(adapter.getAgent(agentId))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new OnChainAgentView(ownerId, NEW_METADATA_HASH, POLICY_HASH, true, 0L));
    }

    @Test
    void shouldRejectStateChangeFromNonRegistrar() {
        // given an agent registered by the registrar
        var registrar = adapterFor(REGISTRAR_KEY);
        var outsider = adapterFor(OUTSIDER_KEY);
        var agentId = UuidCreator.getTimeOrderedEpoch();
        registrar.registerAgent(agentId, UuidCreator.getTimeOrderedEpoch(), METADATA_HASH);

        // when / then a non-registrar wallet cannot mutate it
        assertThatThrownBy(() -> outsider.deactivateAgent(agentId)).isInstanceOf(BlockchainRegistrationException.class);
        assertThat(registrar.isAgentActive(agentId)).isTrue();
    }

    private static String zeroHash() {
        return "0x" + "0".repeat(64);
    }
}
