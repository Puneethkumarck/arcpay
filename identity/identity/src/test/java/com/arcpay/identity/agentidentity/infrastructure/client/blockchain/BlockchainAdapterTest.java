package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.domain.model.OnChainOperation;
import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
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
    private static final String SOME_WALLET = "0x1234567890abcdef1234567890abcdef12345678";
    private static final String CONTRACT_ADDRESS = "0x0000000000000000000000000000000000000abc";
    private static final String TX_HASH = "0x9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    private static final long BLOCK_NUMBER = 4242L;
    private static final long GAS_USED = 73219L;
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(300_000L);
    private static final BigInteger NETWORK_GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    private static final BigInteger RESOLVED_GAS_PRICE = BigInteger.valueOf(40_000_000_000L);

    @Mock
    private Web3j web3j;

    @Mock
    private FastRawTransactionManager transactionManager;

    @Mock
    private TransactionReceiptProcessor receiptProcessor;

    @Mock
    private GasUsageRepository gasUsageRepository;

    @Mock
    @SuppressWarnings("rawtypes")
    private Request ethCallRequest;

    @Mock
    @SuppressWarnings("rawtypes")
    private Request gasPriceRequest;

    @Captor
    private ArgumentCaptor<GasUsage> gasUsageCaptor;

    private final AgentRegistryProperties properties = new AgentRegistryProperties(CONTRACT_ADDRESS, null, null, null);

    private BlockchainAdapter adapter() {
        return new BlockchainAdapter(web3j, transactionManager, receiptProcessor, gasUsageRepository, properties);
    }

    @Test
    void shouldRegisterAgentReturningRealTxHashAndBlockNumberFromReceipt() throws Exception {
        // given
        givenSubmitSucceeds(registerFunction());

        // when
        var result = adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_WALLET, SOME_METADATA_HASH);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(new RegistrationResult(TX_HASH, BLOCK_NUMBER));
    }

    @Test
    void shouldFallBackToConfiguredFloorWhenNetworkQuoteBelowFloor() throws Exception {
        // given a network quote so low that even after the safety multiplier it stays below the floor
        givenNetworkGasPrice(BigInteger.valueOf(100L));
        var accepted = new EthSendTransaction();
        accepted.setResult(TX_HASH);
        given(transactionManager.sendTransaction(
                        GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(registerFunction()),
                        BigInteger.ZERO))
                .willReturn(accepted);
        given(receiptProcessor.waitForTransactionReceipt(TX_HASH)).willReturn(receipt());

        // when
        var result = adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_WALLET, SOME_METADATA_HASH);

        // then the configured floor price is used (the stub matches only GAS_PRICE)
        assertThat(result).usingRecursiveComparison().isEqualTo(new RegistrationResult(TX_HASH, BLOCK_NUMBER));
    }

    @Test
    void shouldRecordGasUsageFromReceiptNotHardcoded() throws Exception {
        // given
        givenSubmitSucceeds(registerFunction());

        // when
        adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_WALLET, SOME_METADATA_HASH);

        // then
        then(gasUsageRepository).should().save(gasUsageCaptor.capture());
        var expected = GasUsage.builder()
                .ownerId(SOME_OWNER_ID)
                .agentId(SOME_AGENT_ID)
                .operation("REGISTER_AGENT")
                .txHash(TX_HASH)
                .gasUsed(GAS_USED)
                .gasCostUsdc(BigDecimal.ZERO)
                .build();
        assertThat(gasUsageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @Test
    void shouldThrowBlockchainRegistrationExceptionWhenSubmissionReturnsError() throws Exception {
        // given
        givenNetworkGasPrice(NETWORK_GAS_PRICE);
        var errored = new EthSendTransaction();
        errored.setError(new Response.Error(-32000, "execution reverted: not registrar"));
        given(transactionManager.sendTransaction(
                        RESOLVED_GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(registerFunction()),
                        BigInteger.ZERO))
                .willReturn(errored);

        // when / then
        assertThatThrownBy(() -> adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_WALLET, SOME_METADATA_HASH))
                .isInstanceOf(BlockchainRegistrationException.class)
                .hasMessageContaining(SOME_AGENT_ID.toString())
                .hasMessageContaining("not registrar");
        then(transactionManager).should().resetNonce();
    }

    @Test
    void shouldWrapReceiptWaitFailureAndResetNonce() throws Exception {
        // given
        givenNetworkGasPrice(NETWORK_GAS_PRICE);
        var accepted = new EthSendTransaction();
        accepted.setResult(TX_HASH);
        given(transactionManager.sendTransaction(
                        RESOLVED_GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(registerFunction()),
                        BigInteger.ZERO))
                .willReturn(accepted);
        given(receiptProcessor.waitForTransactionReceipt(TX_HASH)).willThrow(new IOException("receipt timeout"));

        // when / then
        assertThatThrownBy(() -> adapter().registerAgent(SOME_AGENT_ID, SOME_OWNER_ID, SOME_WALLET, SOME_METADATA_HASH))
                .isInstanceOf(BlockchainRegistrationException.class)
                .hasCauseInstanceOf(IOException.class);
        then(transactionManager).should().resetNonce();
    }

    @Test
    void shouldDeactivateReturningTxHashAndRecordingGas() throws Exception {
        // given
        givenSubmitSucceeds(stateChangeFunction("deactivateAgent"));
        givenGetAgentReturnsOwner();

        // when
        var txHash = adapter().deactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
        assertGasRecorded(OnChainOperation.DEACTIVATE.name());
    }

    @Test
    void shouldReactivateReturningTxHashAndRecordingGas() throws Exception {
        // given
        givenSubmitSucceeds(stateChangeFunction("reactivateAgent"));
        givenGetAgentReturnsOwner();

        // when
        var txHash = adapter().reactivateAgent(SOME_AGENT_ID);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
        assertGasRecorded(OnChainOperation.REACTIVATE.name());
    }

    @Test
    void shouldUpdateMetadataReturningTxHashAndRecordingGas() throws Exception {
        // given
        givenSubmitSucceeds(hashFunction("updateMetadata", SOME_METADATA_HASH));
        givenGetAgentReturnsOwner();

        // when
        var txHash = adapter().updateMetadata(SOME_AGENT_ID, SOME_METADATA_HASH);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
        assertGasRecorded(OnChainOperation.UPDATE_METADATA.name());
    }

    @Test
    void shouldUpdatePolicyReturningTxHashAndRecordingGas() throws Exception {
        // given
        givenSubmitSucceeds(hashFunction("updatePolicy", SOME_POLICY_HASH));
        givenGetAgentReturnsOwner();

        // when
        var txHash = adapter().updatePolicy(SOME_AGENT_ID, SOME_POLICY_HASH);

        // then
        assertThat(txHash).isEqualTo(TX_HASH);
        assertGasRecorded(OnChainOperation.UPDATE_POLICY.name());
    }

    private void assertGasRecorded(String operation) {
        then(gasUsageRepository).should().save(gasUsageCaptor.capture());
        var expected = GasUsage.builder()
                .ownerId(SOME_OWNER_ID)
                .agentId(SOME_AGENT_ID)
                .operation(operation)
                .txHash(TX_HASH)
                .gasUsed(GAS_USED)
                .gasCostUsdc(BigDecimal.ZERO)
                .build();
        assertThat(gasUsageCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt")
                .isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    private void givenGetAgentReturnsOwner() throws Exception {
        var expectedCall = Transaction.createEthCallTransaction(
                null, CONTRACT_ADDRESS, FunctionEncoder.encode(getAgentFunction()));
        var response = new EthCall();
        response.setResult(encodedGetAgentReturn());
        given(web3j.ethCall(eqIgnoring(expectedCall), eqIgnoring(DefaultBlockParameterName.LATEST)))
                .willReturn(ethCallRequest);
        given(ethCallRequest.send()).willReturn(response);
    }

    private Function getAgentFunction() {
        return new Function(
                "getAgent",
                List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID))),
                List.of(
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Uint64>() {}));
    }

    private String encodedGetAgentReturn() {
        return "0x"
                + TypeEncoder.encode(new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_OWNER_ID)))
                + TypeEncoder.encode(new Address(SOME_WALLET))
                + TypeEncoder.encode(new Bytes32(Numeric.hexStringToByteArray(SOME_METADATA_HASH)))
                + TypeEncoder.encode(new Bytes32(new byte[32]))
                + TypeEncoder.encode(new Bool(true))
                + TypeEncoder.encode(new Uint64(BigInteger.ZERO));
    }

    private void givenSubmitSucceeds(Function function) throws Exception {
        givenNetworkGasPrice(NETWORK_GAS_PRICE);
        var accepted = new EthSendTransaction();
        accepted.setResult(TX_HASH);
        given(transactionManager.sendTransaction(
                        RESOLVED_GAS_PRICE,
                        GAS_LIMIT,
                        CONTRACT_ADDRESS,
                        FunctionEncoder.encode(function),
                        BigInteger.ZERO))
                .willReturn(accepted);
        given(receiptProcessor.waitForTransactionReceipt(TX_HASH)).willReturn(receipt());
    }

    @SuppressWarnings("unchecked")
    private void givenNetworkGasPrice(BigInteger price) throws Exception {
        var response = new EthGasPrice();
        response.setResult(Numeric.encodeQuantity(price));
        given(web3j.ethGasPrice()).willReturn(gasPriceRequest);
        given(gasPriceRequest.send()).willReturn(response);
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
                        new Address(SOME_WALLET),
                        new Bytes32(Numeric.hexStringToByteArray(SOME_METADATA_HASH))),
                emptyList());
    }

    private Function stateChangeFunction(String name) {
        return new Function(name, List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID))), emptyList());
    }

    private Function hashFunction(String name, String hash) {
        return new Function(
                name,
                List.of(
                        new Bytes32(UuidConversionUtil.uuidToBytes32(SOME_AGENT_ID)),
                        new Bytes32(Numeric.hexStringToByteArray(hash))),
                emptyList());
    }
}
