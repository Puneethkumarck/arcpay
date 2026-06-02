package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

@ExtendWith(MockitoExtension.class)
class BlockchainAdapterTest {

    private static final UUID SOME_AGENT_ID = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
    private static final UUID SOME_OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    private static final String SOME_METADATA_HASH =
            "0xabababababababababababababababababababababababababababababababab";
    private static final String SOME_POLICY_HASH = "0xcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd";
    private static final String CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000abc";
    private static final String TX_HASH = "0x9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final long BLOCK_NUMBER = 4242L;
    private static final long GAS_USED = 73219L;
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(300_000L);
    private static final Instant FIXED_NOW = Instant.parse("2026-06-02T10:00:00Z");
    private static final BigInteger EPOCH = BigInteger.valueOf(FIXED_NOW.getEpochSecond());

    @Mock
    private Web3j web3j;

    @Mock
    private FastRawTransactionManager transactionManager;

    @Mock
    private TransactionReceiptProcessor receiptProcessor;

    @Mock
    private GasUsageRepository gasUsageRepository;

    @Captor
    private ArgumentCaptor<GasUsage> gasUsageCaptor;

    private final AgentRegistryProperties properties = new AgentRegistryProperties(CONTRACT_ADDRESS, null, null);
    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private BlockchainAdapter adapter() {
        return new BlockchainAdapter(
                web3j, transactionManager, receiptProcessor, gasUsageRepository, properties, clock);
    }

    @Test
    void shouldRegisterAgentReturningRealTxHashAndBlockNumberFromReceipt() throws Exception {
        // given
        givenSubmitSucceeds(registerFunction());

        // when
        var result = adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(new RegistrationResult(TX_HASH, BLOCK_NUMBER));
    }

    @Test
    void shouldRecordGasUsageFromReceiptNotHardcoded() throws Exception {
        // given
        givenSubmitSucceeds(registerFunction());

        // when
        adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH);

        // then
        then(gasUsageRepository).should().save(gasUsageCaptor.capture());
        var expected = GasUsage.builder()
                .ownerId(SOME_OWNER_ID)
                .agentId(SOME_AGENT_ID)
                .operation("REGISTER_AGENT")
                .txHash(TX_HASH)
                .gasUsed(GAS_USED)
                .gasCostUsdc(BigDecimal.ZERO)
                .createdAt(FIXED_NOW)
                .build();
        assertThat(gasUsageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowBlockchainRegistrationExceptionWhenSubmissionReturnsError() throws Exception {
        // given
        var errored = new EthSendTransaction();
        errored.setError(new Response.Error(-32000, "execution reverted: not registrar"));
        given(transactionManager.sendTransaction(
                        GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(registerFunction()),
                        BigInteger.ZERO))
                .willReturn(errored);

        // when / then
        assertThatThrownBy(() -> adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .isInstanceOf(BlockchainRegistrationException.class)
                .hasMessageContaining(SOME_AGENT_ID.toString())
                .hasMessageContaining("not registrar");
    }

    @Test
    void shouldWrapReceiptWaitFailureAndResetNonce() throws Exception {
        // given
        var accepted = new EthSendTransaction();
        accepted.setResult(TX_HASH);
        given(transactionManager.sendTransaction(
                        GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(registerFunction()),
                        BigInteger.ZERO))
                .willReturn(accepted);
        given(receiptProcessor.waitForTransactionReceipt(TX_HASH)).willThrow(new IOException("receipt timeout"));

        // when / then
        assertThatThrownBy(() -> adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_METADATA_HASH))
                .isInstanceOf(BlockchainRegistrationException.class)
                .hasCauseInstanceOf(IOException.class);
        then(transactionManager).should().resetNonce();
    }

    @Test
    void shouldReturnRealTxHashForDeactivate() throws Exception {
        // given
        givenSubmitSucceeds(stateChangeFunction("deactivateAgent"));

        // when
        var txHash = adapter().deactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
    }

    @Test
    void shouldReturnRealTxHashForReactivate() throws Exception {
        // given
        givenSubmitSucceeds(stateChangeFunction("reactivateAgent"));

        // when
        var txHash = adapter().reactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
    }

    @Test
    void shouldReturnRealTxHashForUpdateMetadata() throws Exception {
        // given
        givenSubmitSucceeds(hashFunction("updateMetadata", SOME_METADATA_HASH));

        // when
        var txHash = adapter().updateMetadata(SOME_AGENT_ID, SOME_METADATA_HASH);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
    }

    @Test
    void shouldReturnRealTxHashForUpdatePolicy() throws Exception {
        // given
        givenSubmitSucceeds(hashFunction("updatePolicy", SOME_POLICY_HASH));

        // when
        var txHash = adapter().updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
    }

    private void givenSubmitSucceeds(Function function) throws Exception {
        var accepted = new EthSendTransaction();
        accepted.setResult(TX_HASH);
        given(transactionManager.sendTransaction(
                        GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS, FunctionEncoder.encode(function), BigInteger.ZERO))
                .willReturn(accepted);
        given(receiptProcessor.waitForTransactionReceipt(TX_HASH)).willReturn(receipt());
    }

    private TransactionReceipt receipt() {
        var receipt = new TransactionReceipt();
        receipt.setTransactionHash(TX_HASH);
        receipt.setStatus("0x1");
        receipt.setBlockNumber(Numeric.encodeQuantity(BigInteger.valueOf(BLOCK_NUMBER)));
        receipt.setGasUsed(Numeric.encodeQuantity(BigInteger.valueOf(GAS_USED)));
        return receipt;
    }

    private Function registerFunction() {
        return new Function(
                "registerAgent",
                List.of(
                        new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID)),
                        new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_OWNER_ID)),
                        new Bytes32(Numeric.hexStringToByteArray(SOME_METADATA_HASH)),
                        new Uint64(EPOCH)),
                emptyList());
    }

    private Function stateChangeFunction(String name) {
        return new Function(
                name,
                List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID)), new Uint64(EPOCH)),
                emptyList());
    }

    private Function hashFunction(String name, String hash) {
        return new Function(
                name,
                List.of(
                        new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID)),
                        new Bytes32(Numeric.hexStringToByteArray(hash)),
                        new Uint64(EPOCH)),
                emptyList());
    }
}
