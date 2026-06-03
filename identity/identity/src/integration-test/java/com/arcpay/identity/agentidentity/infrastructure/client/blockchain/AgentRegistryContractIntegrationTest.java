package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
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
    private static final BigInteger CALL_GAS_LIMIT = BigInteger.valueOf(300_000L);

    private static final String REGISTRAR_KEY = "0x4f3edf983ac636a65a842ce7c78d9aa706d3b113bce9c46f30d7d21715b23b1d";
    private static final String OUTSIDER_KEY = "0x6cbed15c793ce57650b9877cf6fa156fbef513c4e6134f022a85b1ffdd59b2a1";

    private static final String AGENT_WALLET = "0x1234567890abcdef1234567890abcdef12345678";
    private static final String OTHER_WALLET = "0x9999999999999999999999999999999999999999";
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

    @BeforeAll
    static void connect() {
        web3j = Web3j.build(new HttpService("http://" + GANACHE.getHost() + ":" + GANACHE.getMappedPort(8545)));
        receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 200L, 60);
    }

    @Test
    void shouldRoundTripAgentLifecycleOnChain() throws Exception {
        // given a freshly deployed registry
        var adapter = adapterFor(REGISTRAR_KEY, deploy(managerFor(REGISTRAR_KEY)));
        var agentId = UuidCreator.getTimeOrderedEpoch();
        var ownerId = UuidCreator.getTimeOrderedEpoch();

        // when registered
        var registration = adapter.registerAgent(agentId, ownerId, AGENT_WALLET, METADATA_HASH);

        // then the on-chain record reflects an active agent bound to its wallet, with real tx coordinates
        assertThat(registration.txHash()).startsWith("0x").hasSize(66);
        assertThat(registration.blockNumber()).isPositive();
        assertThat(adapter.isAgentActive(agentId)).isTrue();
        assertThat(adapter.getAgent(agentId))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new OnChainAgentView(ownerId, AGENT_WALLET, METADATA_HASH, zeroHash(), true, 0L));

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
                .isEqualTo(new OnChainAgentView(ownerId, AGENT_WALLET, NEW_METADATA_HASH, POLICY_HASH, true, 0L));
    }

    @Test
    void shouldResolveWalletToActiveAgentViaReverseIndex() throws Exception {
        // given a registered agent bound to a wallet
        var adapter = adapterFor(REGISTRAR_KEY, deploy(managerFor(REGISTRAR_KEY)));
        var agentId = UuidCreator.getTimeOrderedEpoch();
        adapter.registerAgent(agentId, UuidCreator.getTimeOrderedEpoch(), AGENT_WALLET, METADATA_HASH);

        // then an external party can resolve the wallet back to the active agent
        assertThat(adapter.getAgentByWallet(AGENT_WALLET)).contains(agentId);
        assertThat(adapter.getAgentByWallet(OTHER_WALLET)).isEmpty();
        assertThat(adapter.isWalletActive(AGENT_WALLET)).isTrue();

        adapter.deactivateAgent(agentId);
        assertThat(adapter.isWalletActive(AGENT_WALLET)).isFalse();
    }

    @Test
    void shouldTreatDuplicateRegistrationAsIdempotentButRejectConflicting() throws Exception {
        // given a registered agent
        var adapter = adapterFor(REGISTRAR_KEY, deploy(managerFor(REGISTRAR_KEY)));
        var agentId = UuidCreator.getTimeOrderedEpoch();
        var ownerId = UuidCreator.getTimeOrderedEpoch();
        adapter.registerAgent(agentId, ownerId, AGENT_WALLET, METADATA_HASH);

        // when the same registration is re-submitted (workflow-retry safe)
        adapter.registerAgent(agentId, ownerId, AGENT_WALLET, METADATA_HASH);

        // then it is a no-op success, and a conflicting re-registration is rejected
        assertThat(adapter.isAgentActive(agentId)).isTrue();
        assertThatThrownBy(() -> adapter.registerAgent(agentId, ownerId, OTHER_WALLET, METADATA_HASH))
                .isInstanceOf(BlockchainRegistrationException.class);
    }

    @Test
    void shouldRejectStateChangeFromNonRegistrar() throws Exception {
        // given an agent registered by the registrar
        var address = deploy(managerFor(REGISTRAR_KEY));
        var registrar = adapterFor(REGISTRAR_KEY, address);
        var outsider = adapterFor(OUTSIDER_KEY, address);
        var agentId = UuidCreator.getTimeOrderedEpoch();
        registrar.registerAgent(agentId, UuidCreator.getTimeOrderedEpoch(), AGENT_WALLET, METADATA_HASH);

        // when / then a non-registrar wallet cannot mutate it
        assertThatThrownBy(() -> outsider.deactivateAgent(agentId)).isInstanceOf(BlockchainRegistrationException.class);
        assertThat(registrar.isAgentActive(agentId)).isTrue();
    }

    @Test
    void shouldRotateRegistrarViaTwoStepTransfer() throws Exception {
        // given an isolated registry and a pending transfer to the outsider
        var registrarManager = managerFor(REGISTRAR_KEY);
        var outsiderManager = managerFor(OUTSIDER_KEY);
        var isolated = deploy(registrarManager);
        var outsiderAddress = Credentials.create(OUTSIDER_KEY).getAddress();

        // when the registrar role is transferred and accepted
        send(
                registrarManager,
                isolated,
                new Function("transferRegistrar", List.of(new Address(outsiderAddress)), emptyList()));
        send(outsiderManager, isolated, new Function("acceptRegistrar", emptyList(), emptyList()));

        // then the new registrar can write and the previous registrar can no longer write
        var newRegistrar = adapterFor(OUTSIDER_KEY, isolated);
        var previousRegistrar = adapterFor(REGISTRAR_KEY, isolated);
        var agentId = UuidCreator.getTimeOrderedEpoch();
        newRegistrar.registerAgent(agentId, UuidCreator.getTimeOrderedEpoch(), AGENT_WALLET, METADATA_HASH);
        assertThat(newRegistrar.isAgentActive(agentId)).isTrue();
        assertThatThrownBy(() -> previousRegistrar.deactivateAgent(agentId))
                .isInstanceOf(BlockchainRegistrationException.class);
    }

    private static FastRawTransactionManager managerFor(String privateKey) {
        return new FastRawTransactionManager(web3j, Credentials.create(privateKey), CHAIN_ID);
    }

    private BlockchainAdapter adapterFor(String privateKey, String address) {
        var properties = new AgentRegistryProperties(address, null, GAS_PRICE, null);
        return new BlockchainAdapter(
                web3j, managerFor(privateKey), receiptProcessor, mock(GasUsageRepository.class), properties);
    }

    private static String deploy(FastRawTransactionManager registrarManager) throws Exception {
        var bytecode = Files.readString(Path.of("contracts/AgentRegistry.bin")).trim();
        var deploy = registrarManager.sendTransaction(GAS_PRICE, DEPLOY_GAS_LIMIT, "", bytecode, BigInteger.ZERO);
        return receiptProcessor
                .waitForTransactionReceipt(deploy.getTransactionHash())
                .getContractAddress();
    }

    private static void send(FastRawTransactionManager manager, String address, Function function) throws Exception {
        var response = manager.sendTransaction(
                GAS_PRICE, CALL_GAS_LIMIT, address, FunctionEncoder.encode(function), BigInteger.ZERO);
        receiptProcessor.waitForTransactionReceipt(response.getTransactionHash());
    }

    private static String zeroHash() {
        return "0x" + "0".repeat(64);
    }
}
