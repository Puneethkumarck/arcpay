package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import static java.util.Collections.emptyList;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.domain.model.OnChainOperation;
import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Numeric;

@Slf4j
@Component
@RequiredArgsConstructor
class BlockchainAdapter implements BlockchainService {

    private static final Pattern BYTES32_HEX = Pattern.compile("^0x[0-9a-fA-F]{64}$");

    private final Web3j web3j;
    private final FastRawTransactionManager transactionManager;
    private final TransactionReceiptProcessor receiptProcessor;
    private final GasUsageRepository gasUsageRepository;
    private final AgentRegistryProperties properties;

    private final ReentrantLock nonceSerializationLock = new ReentrantLock(true);

    @Override
    public RegistrationResult registerAgent(UUID agentId, UUID ownerId, String walletAddress, String metadataHash) {
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new BlockchainRegistrationException(
                    "AgentRegistry.registerAgent requires a wallet address for agentId=" + agentId);
        }
        var function = new Function(
                "registerAgent",
                List.of(
                        new Bytes32(UuidConversionUtil.uuidToBytes32(agentId)),
                        new Bytes32(UuidConversionUtil.uuidToBytes32(ownerId)),
                        new Address(walletAddress),
                        new Bytes32(hashToBytes32(metadataHash))),
                emptyList());
        var receipt = submit(function, agentId);
        recordGasUsage(ownerId, agentId, "REGISTER_AGENT", receipt.getTransactionHash(), receipt.getGasUsed());
        return new RegistrationResult(
                receipt.getTransactionHash(), receipt.getBlockNumber().longValue());
    }

    @Override
    public String deactivateAgent(UUID agentId) {
        var function = new Function(
                "deactivateAgent", List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(agentId))), emptyList());
        return submitMutation(function, agentId, OnChainOperation.DEACTIVATE);
    }

    @Override
    public String reactivateAgent(UUID agentId) {
        var function = new Function(
                "reactivateAgent", List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(agentId))), emptyList());
        return submitMutation(function, agentId, OnChainOperation.REACTIVATE);
    }

    @Override
    public String updateMetadata(UUID agentId, String metadataHash) {
        var function = new Function(
                "updateMetadata",
                List.of(
                        new Bytes32(UuidConversionUtil.uuidToBytes32(agentId)),
                        new Bytes32(hashToBytes32(metadataHash))),
                emptyList());
        return submitMutation(function, agentId, OnChainOperation.UPDATE_METADATA);
    }

    @Override
    public String updatePolicy(UUID agentId, String policyHash) {
        var function = new Function(
                "updatePolicy",
                List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(agentId)), new Bytes32(hashToBytes32(policyHash))),
                emptyList());
        return submitMutation(function, agentId, OnChainOperation.UPDATE_POLICY);
    }

    private String submitMutation(Function function, UUID agentId, OnChainOperation operation) {
        var receipt = submit(function, agentId);
        var ownerId = getAgent(agentId).owner();
        recordGasUsage(ownerId, agentId, operation.name(), receipt.getTransactionHash(), receipt.getGasUsed());
        return receipt.getTransactionHash();
    }

    @Override
    public boolean isAgentActive(UUID agentId) {
        var function = new Function(
                "isAgentActive",
                List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(agentId))),
                List.of(new TypeReference<Bool>() {}));
        var decoded = callView(function, agentId.toString());
        return (Boolean) decoded.get(0).getValue();
    }

    OnChainAgentView getAgent(UUID agentId) {
        var function = new Function(
                "getAgent",
                List.of(new Bytes32(UuidConversionUtil.uuidToBytes32(agentId))),
                List.of(
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bool>() {},
                        new TypeReference<Uint64>() {}));
        var decoded = callView(function, agentId.toString());
        return new OnChainAgentView(
                UuidConversionUtil.bytes32ToUuid((byte[]) decoded.get(0).getValue()),
                decoded.get(1).getValue().toString(),
                Numeric.toHexString((byte[]) decoded.get(2).getValue()),
                Numeric.toHexString((byte[]) decoded.get(3).getValue()),
                (Boolean) decoded.get(4).getValue(),
                ((BigInteger) decoded.get(5).getValue()).longValue());
    }

    Optional<UUID> getAgentByWallet(String walletAddress) {
        var function = new Function(
                "getAgentByWallet", List.of(new Address(walletAddress)), List.of(new TypeReference<Bytes32>() {}));
        var decoded = callView(function, walletAddress);
        var agentBytes = (byte[]) decoded.get(0).getValue();
        return isZero(agentBytes) ? Optional.empty() : Optional.of(UuidConversionUtil.bytes32ToUuid(agentBytes));
    }

    boolean isWalletActive(String walletAddress) {
        var function = new Function(
                "isWalletActive", List.of(new Address(walletAddress)), List.of(new TypeReference<Bool>() {}));
        var decoded = callView(function, walletAddress);
        return (Boolean) decoded.get(0).getValue();
    }

    private TransactionReceipt submit(Function function, UUID agentId) {
        nonceSerializationLock.lock();
        try {
            var data = FunctionEncoder.encode(function);
            var response = transactionManager.sendTransaction(
                    resolveGasPrice(), properties.gasLimit(), properties.agentRegistryAddress(), data, BigInteger.ZERO);
            if (response.hasError()) {
                throw new BlockchainRegistrationException("AgentRegistry." + function.getName()
                        + " rejected for agentId=" + agentId + ": "
                        + response.getError().getMessage());
            }
            var receipt = receiptProcessor.waitForTransactionReceipt(response.getTransactionHash());
            if (!receipt.isStatusOK()) {
                throw new BlockchainRegistrationException("AgentRegistry." + function.getName()
                        + " reverted on-chain for agentId=" + agentId + " txHash=" + receipt.getTransactionHash()
                        + " status=" + receipt.getStatus());
            }
            log.info(
                    "On-chain {} agentId={} txHash={} block={} gasUsed={}",
                    function.getName(),
                    agentId,
                    receipt.getTransactionHash(),
                    receipt.getBlockNumber(),
                    receipt.getGasUsed());
            return receipt;
        } catch (BlockchainRegistrationException e) {
            resetNonceQuietly();
            throw e;
        } catch (Exception e) {
            resetNonceQuietly();
            throw new BlockchainRegistrationException(
                    "AgentRegistry." + function.getName() + " failed for agentId=" + agentId, e);
        } finally {
            nonceSerializationLock.unlock();
        }
    }

    private List<Type> callView(Function function, String context) {
        try {
            var data = FunctionEncoder.encode(function);
            var response = web3j.ethCall(
                            Transaction.createEthCallTransaction(
                                    transactionManager.getFromAddress(), properties.agentRegistryAddress(), data),
                            DefaultBlockParameterName.LATEST)
                    .send();
            if (response.isReverted()) {
                throw new BlockchainRegistrationException("AgentRegistry." + function.getName() + " reverted for "
                        + context + ": " + response.getRevertReason());
            }
            var decoded = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());
            if (decoded.size() != function.getOutputParameters().size()) {
                throw new BlockchainRegistrationException("AgentRegistry." + function.getName()
                        + " returned no decodable value for " + context + " (empty or malformed response)");
            }
            return decoded;
        } catch (BlockchainRegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new BlockchainRegistrationException(
                    "AgentRegistry." + function.getName() + " view call failed for " + context, e);
        }
    }

    private BigInteger resolveGasPrice() {
        try {
            var networkGasPrice = web3j.ethGasPrice().send().getGasPrice();
            var bumpedNetworkPrice = networkGasPrice
                    .multiply(BigInteger.valueOf(properties.gasPriceMultiplierPercent()))
                    .divide(BigInteger.valueOf(100L));
            return bumpedNetworkPrice.max(properties.gasPrice());
        } catch (Exception e) {
            log.warn(
                    "Failed to fetch network gas price, falling back to configured floor {} wei: {}",
                    properties.gasPrice(),
                    e.getMessage());
            return properties.gasPrice();
        }
    }

    private void resetNonceQuietly() {
        try {
            transactionManager.resetNonce();
        } catch (Exception e) {
            log.warn("Failed to resync platform-wallet nonce after on-chain failure: {}", e.getMessage());
        }
    }

    private boolean isZero(byte[] bytes) {
        for (var b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private byte[] hashToBytes32(String hash) {
        if (hash != null && BYTES32_HEX.matcher(hash).matches()) {
            return Numeric.hexStringToByteArray(hash);
        }
        return Hash.sha3((hash == null ? "" : hash).getBytes(StandardCharsets.UTF_8));
    }

    private void recordGasUsage(UUID ownerId, UUID agentId, String operation, String txHash, BigInteger gasUsed) {
        var gasUsage = GasUsage.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .ownerId(ownerId)
                .agentId(agentId)
                .operation(operation)
                .txHash(txHash)
                .gasUsed(gasUsed.longValue())
                .gasCostUsdc(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();
        gasUsageRepository.save(gasUsage);
    }
}
