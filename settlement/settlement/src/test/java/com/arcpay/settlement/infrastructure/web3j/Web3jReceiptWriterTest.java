package com.arcpay.settlement.infrastructure.web3j;

import com.arcpay.settlement.domain.model.ReceiptCommand;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.FastRawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.someReceiptCommand;
import static com.arcpay.settlement.fixtures.ReceiptCommandFixtures.someReceiptCommandWithoutMemo;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class Web3jReceiptWriterTest {

    private static final String CONTRACT_ADDRESS = "0x3333333333333333333333333333333333333333";
    private static final String GAS_WALLET_ADDRESS = "0x4444444444444444444444444444444444444444";
    private static final String SOME_TX_HASH = "0xabc123";
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(1_000_000_000L);
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(150_000L);
    private static final BigInteger LOW_BALANCE_THRESHOLD = BigInteger.valueOf(1_000L);
    private static final long FIXED_EPOCH_SECONDS = Instant.parse("2026-05-30T10:00:00Z").getEpochSecond();
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-30T10:00:00Z"), ZoneOffset.UTC);
    private static final byte[] ZERO_HASH = new byte[32];

    @Mock
    private Web3j web3j;

    @Mock
    private FastRawTransactionManager transactionManager;

    @Mock
    @SuppressWarnings("rawtypes")
    private Request balanceRequest;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ReceiptContractProperties properties() {
        return new ReceiptContractProperties(CONTRACT_ADDRESS, 999L, GAS_LIMIT, GAS_PRICE, LOW_BALANCE_THRESHOLD);
    }

    private Web3jReceiptWriter writer() {
        return new Web3jReceiptWriter(web3j, transactionManager, properties(), meterRegistry, FIXED_CLOCK);
    }

    private String expectedData(ReceiptCommand command, byte[] memoHash) {
        var paymentId = new byte[32];
        var high = command.paymentId().getMostSignificantBits();
        var low = command.paymentId().getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            paymentId[16 + i] = (byte) (high >>> (56 - 8 * i));
            paymentId[24 + i] = (byte) (low >>> (56 - 8 * i));
        }
        var function = new Function(
                "recordReceipt",
                List.of(
                        new Bytes32(paymentId),
                        new Address(command.payerAgent()),
                        new Address(command.payee()),
                        new Uint256(command.amount().movePointRight(6).toBigIntegerExact()),
                        new Bytes32(memoHash),
                        new Uint64(BigInteger.valueOf(FIXED_EPOCH_SECONDS))),
                emptyList());
        return FunctionEncoder.encode(function);
    }

    @SuppressWarnings("unchecked")
    private void stubBalance(String balanceHex) throws IOException {
        var balanceResponse = new EthGetBalance();
        balanceResponse.setResult(balanceHex);
        lenient().when(web3j.ethGetBalance(GAS_WALLET_ADDRESS, DefaultBlockParameter.valueOf("latest")))
                .thenReturn(balanceRequest);
        lenient().when(balanceRequest.send()).thenReturn(balanceResponse);
        lenient().when(transactionManager.getFromAddress()).thenReturn(GAS_WALLET_ADDRESS);
    }

    private EthSendTransaction successResponse() {
        var response = new EthSendTransaction();
        response.setResult(SOME_TX_HASH);
        return response;
    }

    @Test
    void shouldReturnOnChainRefOnSuccessWithKeccakMemoHash() throws Exception {
        // given
        stubBalance("0xffff");
        var command = someReceiptCommand();
        var memoHash = Hash.sha3(command.memo().getBytes(StandardCharsets.UTF_8));
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, memoHash), BigInteger.ZERO))
                .willReturn(successResponse());

        // when
        var onChainRef = writer().writeReceipt(command);

        // then
        assertThat(onChainRef).isEqualTo(SOME_TX_HASH);
    }

    @Test
    void shouldEncodeZeroMemoHashWhenMemoNull() throws Exception {
        // given
        stubBalance("0xffff");
        var command = someReceiptCommandWithoutMemo();
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, ZERO_HASH), BigInteger.ZERO))
                .willReturn(successResponse());

        // when
        var onChainRef = writer().writeReceipt(command);

        // then
        assertThat(onChainRef).isEqualTo(SOME_TX_HASH);
    }

    @Test
    void shouldSwallowPermanentFailureWithoutOnChainRef() throws Exception {
        // given
        stubBalance("0xffff");
        var command = someReceiptCommand();
        var memoHash = Hash.sha3(command.memo().getBytes(StandardCharsets.UTF_8));
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, memoHash), BigInteger.ZERO))
                .willThrow(new IOException("rpc unreachable"));

        // when
        var onChainRef = writer().writeReceipt(command);

        // then
        assertThat(onChainRef).isNull();
        then(transactionManager).should().resetNonce();
    }

    @Test
    void shouldReturnNullWhenSubmissionResponseHasError() throws Exception {
        // given
        stubBalance("0xffff");
        var command = someReceiptCommand();
        var memoHash = Hash.sha3(command.memo().getBytes(StandardCharsets.UTF_8));
        var errored = new EthSendTransaction();
        errored.setError(new Response.Error(-32000, "nonce too low"));
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, memoHash), BigInteger.ZERO))
                .willReturn(errored);

        // when
        var onChainRef = writer().writeReceipt(command);

        // then
        assertThat(onChainRef).isNull();
    }

    @Test
    void shouldEmitLowBalanceMetricWhenGasWalletBelowThreshold() throws Exception {
        // given
        stubBalance("0x1");
        var command = someReceiptCommand();
        var memoHash = Hash.sha3(command.memo().getBytes(StandardCharsets.UTF_8));
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, memoHash), BigInteger.ZERO))
                .willReturn(successResponse());

        // when
        writer().writeReceipt(command);

        // then
        assertThat(meterRegistry.counter("settlement.receipt.gas_wallet.low_balance").count()).isEqualTo(1.0);
    }

    @Test
    void shouldSerializeConcurrentWritesThroughSingleSigner() throws Exception {
        // given
        var concurrentEntries = new AtomicInteger();
        var nonceCollision = new AtomicBoolean(false);
        var command = someReceiptCommand();
        var memoHash = Hash.sha3(command.memo().getBytes(StandardCharsets.UTF_8));
        stubBalance("0xffff");
        given(transactionManager.sendTransaction(GAS_PRICE, GAS_LIMIT, CONTRACT_ADDRESS,
                expectedData(command, memoHash), BigInteger.ZERO))
                .willAnswer(invocation -> {
                    if (concurrentEntries.incrementAndGet() > 1) {
                        nonceCollision.set(true);
                    }
                    Thread.sleep(20);
                    concurrentEntries.decrementAndGet();
                    return successResponse();
                });
        var writer = writer();
        var threads = 8;
        var ready = new CountDownLatch(threads);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // when
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    writer.writeReceipt(command);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // then
        assertThat(nonceCollision).isFalse();
    }
}
