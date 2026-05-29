package com.arcpay.policy.policyengine.domain.policy;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient;
import com.arcpay.policy.policyengine.domain.port.AgentServiceClient.AgentInfo;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.domain.port.PolicyRepository;
import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyCommandHandler {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AgentServiceClient agentServiceClient;
    private final PolicyValidator policyValidator;
    private final PolicyCreationService policyCreationService;
    private final PolicyRepository policyRepository;
    private final SpendingLockRepository spendingLockRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public Policy createOrUpdatePolicy(UUID agentId, OwnerPrincipal principal, List<PolicyRule> rules) {
        var ownerId = principal.ownerId();
        verifyOwnershipAndActive(agentId, ownerId);
        policyValidator.validate(rules);
        var policyHash = PolicyHashUtil.computePolicyHash(rules);

        spendingLockRepository.createIfNotExists(agentId);
        spendingLockRepository.acquireLock(agentId);

        var existing = policyRepository.findActiveByAgentId(agentId);
        if (existing.isPresent() && existing.get().policyHash().equals(policyHash)) {
            log.info("Policy unchanged, returning existing agentId={} policyId={} hash={}",
                    agentId, existing.get().policyId(), policyHash);
            return existing.get();
        }

        existing.ifPresent(active -> {
            var superseded = policyRepository.save(active.supersede());
            log.info("Superseded policy agentId={} policyId={} version={}",
                    agentId, superseded.policyId(), superseded.version());
        });

        var nextVersion = policyRepository.findMaxVersionByAgentId(agentId).orElse(0) + 1;
        var policy = policyCreationService.createPolicy(agentId, ownerId, rules, policyHash, nextVersion);
        var savedPolicy = policyRepository.save(policy);

        agentServiceClient.updatePolicy(agentId, policyHash);

        eventPublisher.publish(new PolicyCreated(
                savedPolicy.policyId(),
                savedPolicy.agentId(),
                savedPolicy.ownerId(),
                savedPolicy.version(),
                savedPolicy.policyHash(),
                savedPolicy.createdAt()));

        log.info("Policy created agentId={} policyId={} version={} hash={}",
                agentId, savedPolicy.policyId(), savedPolicy.version(), policyHash);
        return savedPolicy;
    }

    private void verifyOwnershipAndActive(UUID agentId, UUID ownerId) {
        var agent = verifyOwnership(agentId, ownerId);
        if (!ACTIVE_STATUS.equals(agent.status())) {
            throw new AgentNotActiveException(agentId, agent.status());
        }
    }

    private AgentInfo verifyOwnership(UUID agentId, UUID ownerId) {
        var agent = agentServiceClient.getAgent(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new AgentOwnershipException(agentId, ownerId);
        }
        return agent;
    }
}
