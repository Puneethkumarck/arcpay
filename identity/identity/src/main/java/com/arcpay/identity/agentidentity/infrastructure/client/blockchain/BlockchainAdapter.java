package com.arcpay.identity.agentidentity.infrastructure.client.blockchain;

import com.arcpay.identity.agentidentity.domain.agent.UuidConversionUtil;
import com.arcpay.identity.agentidentity.domain.exception.BlockchainRegistrationException;
import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.domain.model.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class BlockchainAdapter implements BlockchainService {

    private final Web3j web3j;
    private final GasUsageRepository gasUsageRepository;
    private final String contractAddress;

    @Override
    public RegistrationResult registerAgent(UUID agentId, UUID ownerId, String metadataHash) {
        var agentBytes32 = UuidConversionUtil.uuidToBytes32(agentId);
        try {
            // TODO: Replace with actual AgentRegistry.sol contract call once ABI is generated
            var txHash = "0x" + HexFormat.of().formatHex(agentBytes32);
            var blockNumber = web3j.ethBlockNumber().send().getBlockNumber().longValue();
            log.info("Agent registered on-chain agentId={} txHash={}", agentId, txHash);
            recordGasUsage(ownerId, agentId, "REGISTER_AGENT", txHash, BigInteger.valueOf(21000));
            return new RegistrationResult(txHash, blockNumber);
        } catch (Exception e) {
            log.error("Blockchain registration failed agentId={}: {}", agentId, e.getMessage());
            throw new BlockchainRegistrationException("Blockchain registration failed for agentId=" + agentId, e);
        }
    }

    @Override
    public String deactivateAgent(UUID agentId) {
        // TODO: Implement actual AgentRegistry.sol deactivateAgent() contract call
        log.info("Agent deactivated on-chain agentId={}", agentId);
        return "0xdeactivate_" + agentId;
    }

    @Override
    public String reactivateAgent(UUID agentId) {
        // TODO: Implement actual AgentRegistry.sol reactivateAgent() contract call
        log.info("Agent reactivated on-chain agentId={}", agentId);
        return "0xreactivate_" + agentId;
    }

    @Override
    public String updateMetadata(UUID agentId, String metadataHash) {
        // TODO: Implement actual AgentRegistry.sol updateMetadata() contract call
        log.info("Agent metadata updated on-chain agentId={}", agentId);
        return "0xupdate_metadata_" + agentId;
    }

    @Override
    public String updatePolicy(UUID agentId, String policyHash) {
        // TODO: Implement actual AgentRegistry.sol updatePolicy() contract call
        log.info("Agent policy updated on-chain agentId={}", agentId);
        return "0xupdate_policy_" + agentId;
    }

    @Override
    public boolean isAgentActive(UUID agentId) {
        // TODO: Implement actual AgentRegistry.sol isAgentActive() view call
        return true;
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
