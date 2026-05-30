package com.arcpay.settlement.infrastructure.web3j;

import com.arcpay.settlement.domain.model.ReceiptCommand;
import com.arcpay.settlement.domain.port.ReceiptWriter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.FastRawTransactionManager;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptyList;

@Slf4j
class Web3jReceiptWriter implements ReceiptWriter {

    private static final String RECORD_RECEIPT = "recordReceipt";
    private static final byte[] ZERO_HASH = new byte[32];
    private static final String LOW_BALANCE_METRIC = "settlement.receipt.gas_wallet.low_balance";

    private final Web3j web3j;
    private final FastRawTransactionManager transactionManager;
    private final ReceiptContractProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final ReentrantLock writeLock = new ReentrantLock(true);

    Web3jReceiptWriter(Web3j web3j,
                       FastRawTransactionManager transactionManager,
                       ReceiptContractProperties properties,
                       MeterRegistry meterRegistry,
                       Clock clock) {
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    public String writeReceipt(ReceiptCommand command) {
        writeLock.lock();
        try {
            return submit(command);
        } catch (Exception e) {
            log.warn("On-chain receipt write failed paymentId={}; payment stays COMPLETED, no onChainRef: {}",
                    command.paymentId(), e.getMessage());
            resetNonceQuietly();
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    private void resetNonceQuietly() {
        try {
            transactionManager.resetNonce();
        } catch (Exception e) {
            log.warn("Failed to resync gas-wallet nonce after receipt failure: {}", e.getMessage());
        }
    }

    private String submit(ReceiptCommand command) throws Exception {
        warnOnLowGasBalance();
        var data = FunctionEncoder.encode(buildFunction(command));
        var response = transactionManager.sendTransaction(
                properties.gasPrice(),
                properties.gasLimit(),
                properties.paymentReceiptsAddress(),
                data,
                BigInteger.ZERO);
        if (response.hasError()) {
            throw new ReceiptSubmissionException(response.getError().getMessage());
        }
        var onChainRef = response.getTransactionHash();
        log.info("On-chain receipt recorded paymentId={} onChainRef={}", command.paymentId(), onChainRef);
        return onChainRef;
    }

    private Function buildFunction(ReceiptCommand command) {
        return new Function(
                RECORD_RECEIPT,
                List.of(
                        new Bytes32(paymentIdToBytes32(command.paymentId())),
                        new Address(command.payerAgent()),
                        new Address(command.payee()),
                        new Uint256(toBaseUnits(command.amount())),
                        new Bytes32(memoHash(command.memo())),
                        new Uint64(BigInteger.valueOf(clock.instant().getEpochSecond()))),
                emptyList());
    }

    private byte[] paymentIdToBytes32(java.util.UUID paymentId) {
        var bytes = new byte[32];
        var high = paymentId.getMostSignificantBits();
        var low = paymentId.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[16 + i] = (byte) (high >>> (56 - 8 * i));
        }
        for (int i = 0; i < 8; i++) {
            bytes[24 + i] = (byte) (low >>> (56 - 8 * i));
        }
        return bytes;
    }

    private byte[] memoHash(String memo) {
        if (memo == null) {
            return ZERO_HASH;
        }
        return Hash.sha3(memo.getBytes(StandardCharsets.UTF_8));
    }

    private BigInteger toBaseUnits(java.math.BigDecimal amount) {
        return amount.movePointRight(6).toBigIntegerExact();
    }

    private void warnOnLowGasBalance() {
        try {
            var balance = web3j.ethGetBalance(transactionManager.getFromAddress(), DefaultBlockParameterName.LATEST)
                    .send()
                    .getBalance();
            if (balance.compareTo(properties.lowBalanceThresholdWei()) < 0) {
                meterRegistry.counter(LOW_BALANCE_METRIC).increment();
                log.warn("Gas wallet balance low address={} balanceWei={} thresholdWei={}",
                        transactionManager.getFromAddress(), balance, properties.lowBalanceThresholdWei());
            }
        } catch (Exception e) {
            log.warn("Unable to read gas wallet balance: {}", e.getMessage());
        }
    }
}
