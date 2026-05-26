package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.event.AgentActivated;
import com.arcpay.identity.agentidentity.domain.event.AgentOnChainRegistered;
import com.arcpay.identity.agentidentity.domain.event.AgentProvisioningFailed;
import com.arcpay.identity.agentidentity.domain.event.AgentWalletProvisioned;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotInExpectedStateException;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProvisioningService {

    private final AgentRepository agentRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void completeWalletCreation(UUID agentId, String walletId, String walletAddress) {
        var agent = agentRepository.findByIdForUpdate(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (agent.status() != AgentStatus.PROVISIONING) {
            throw new AgentNotInExpectedStateException(agentId, agent.status(), AgentStatus.PROVISIONING);
        }
        var updatedAgent = agent.withWallet(walletId, walletAddress);
        var savedAgent = agentRepository.save(updatedAgent);
        eventPublisher.publish(new AgentWalletProvisioned(agentId, walletId, walletAddress, savedAgent.updatedAt()));
        log.info("Wallet creation completed agentId={} walletId={}", agentId, walletId);
    }

    @Transactional
    public void completeOnChainRegistration(UUID agentId, String txHash, long blockNumber) {
        var agent = agentRepository.findByIdForUpdate(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (agent.status() != AgentStatus.WALLET_READY) {
            throw new AgentNotInExpectedStateException(agentId, agent.status(), AgentStatus.WALLET_READY);
        }
        var updatedAgent = agent.withOnChainRegistration(txHash);
        var savedAgent = agentRepository.save(updatedAgent);
        eventPublisher.publish(new AgentOnChainRegistered(agentId, txHash, blockNumber, savedAgent.updatedAt()));
        eventPublisher.publish(new AgentActivated(agentId, savedAgent.updatedAt()));
        log.info("On-chain registration completed agentId={} txHash={}", agentId, txHash);
    }

    @Transactional
    public void failProvisioning(UUID agentId, String failedStep, String reason) {
        var agent = agentRepository.findByIdForUpdate(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (agent.status() != AgentStatus.PROVISIONING && agent.status() != AgentStatus.WALLET_READY) {
            throw new AgentNotInExpectedStateException(agentId, agent.status(), AgentStatus.PROVISIONING);
        }
        var updatedAgent = agent.withFailure(reason);
        var savedAgent = agentRepository.save(updatedAgent);
        eventPublisher.publish(new AgentProvisioningFailed(agentId, failedStep, reason, savedAgent.updatedAt()));
        log.info("Provisioning failed agentId={} step={} reason={}", agentId, failedStep, reason);
    }
}
