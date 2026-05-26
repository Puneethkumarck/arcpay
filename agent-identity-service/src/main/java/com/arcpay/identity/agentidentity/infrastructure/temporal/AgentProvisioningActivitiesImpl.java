package com.arcpay.identity.agentidentity.infrastructure.temporal;

import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningActivities;
import com.arcpay.identity.agentidentity.domain.agent.AgentProvisioningService;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "AgentIdentityTaskQueue")
class AgentProvisioningActivitiesImpl implements AgentProvisioningActivities {

    private final CircleWalletService circleWalletService;
    private final BlockchainService blockchainService;
    private final AgentProvisioningService agentProvisioningService;
    private final AgentRepository agentRepository;

    @Override
    public void createCircleWallet(UUID agentId) {
        log.info("Creating Circle wallet for agentId={}", agentId);
        var result = circleWalletService.createWallet(agentId);
        agentProvisioningService.completeWalletCreation(agentId, result.walletId(), result.walletAddress());
        log.info("Wallet created for agentId={} walletId={}", agentId, result.walletId());
    }

    @Override
    public void registerOnChain(UUID agentId) {
        log.info("Registering agent on-chain agentId={}", agentId);
        var agent = agentRepository.findById(agentId)
                .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure(
                        "Agent not found: " + agentId, "AGENT_NOT_FOUND"));
        var result = blockchainService.registerAgent(agentId, agent.ownerId(), agent.metadataHash());
        agentProvisioningService.completeOnChainRegistration(agentId, result.txHash(), result.blockNumber());
        log.info("On-chain registration complete for agentId={} txHash={}", agentId, result.txHash());
    }

    @Override
    public void failProvisioning(UUID agentId, String failedStep, String reason) {
        log.warn("Failing provisioning for agentId={} step={} reason={}", agentId, failedStep, reason);
        agentProvisioningService.failProvisioning(agentId, failedStep, reason);
    }
}
