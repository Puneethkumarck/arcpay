package com.arcpay.identity.agentidentity.domain.agent;

import com.arcpay.identity.agentidentity.domain.event.AgentDeactivated;
import com.arcpay.identity.agentidentity.domain.event.AgentMetadataUpdated;
import com.arcpay.identity.agentidentity.domain.event.AgentPolicyUpdated;
import com.arcpay.identity.agentidentity.domain.event.AgentReactivated;
import com.arcpay.identity.agentidentity.domain.event.AgentRegistrationRequested;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCommandHandler {

    private final AgentValidator agentValidator;
    private final AgentCreationService agentCreationService;
    private final AgentRepository agentRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public Agent registerAgent(UUID ownerId, String name, String purpose, String policyHash) {
        agentValidator.validateRegistration(ownerId, name, purpose, policyHash);
        var agent = agentCreationService.createAgent(ownerId, name, purpose, policyHash);
        var savedAgent = agentRepository.save(agent);
        eventPublisher.publish(new AgentRegistrationRequested(
                savedAgent.agentId(),
                savedAgent.ownerId(),
                savedAgent.name(),
                savedAgent.purpose(),
                savedAgent.metadataHash(),
                savedAgent.createdAt()));
        log.info("Agent registration requested agentId={} ownerId={}", savedAgent.agentId(), ownerId);
        return savedAgent;
    }

    @Transactional
    public Agent updateMetadata(UUID agentId, UUID ownerId, String name, String purpose) {
        var agent = findAgentForUpdate(agentId, ownerId);
        var effectiveName = name != null ? name : agent.name();
        var effectivePurpose = purpose != null ? purpose : agent.purpose();
        agentValidator.validateUpdate(ownerId, agentId, name, purpose);
        var newMetadataHash = MetadataHashUtil.computeMetadataHash(effectiveName, effectivePurpose);
        var updatedAgent = agent.updateMetadata(effectiveName, effectivePurpose, newMetadataHash);
        var savedAgent = agentRepository.save(updatedAgent);
        eventPublisher.publish(new AgentMetadataUpdated(
                savedAgent.agentId(),
                savedAgent.name(),
                savedAgent.purpose(),
                savedAgent.metadataHash(),
                savedAgent.updatedAt()));
        log.info("Agent metadata updated agentId={}", agentId);
        return savedAgent;
    }

    @Transactional
    public Agent deactivate(UUID agentId, UUID ownerId) {
        var agent = findAgentForUpdate(agentId, ownerId);
        var deactivatedAgent = agent.deactivate();
        var savedAgent = agentRepository.save(deactivatedAgent);
        eventPublisher.publish(new AgentDeactivated(savedAgent.agentId(), savedAgent.updatedAt()));
        log.info("Agent deactivated agentId={}", agentId);
        return savedAgent;
    }

    @Transactional
    public Agent reactivate(UUID agentId, UUID ownerId) {
        var agent = findAgentForUpdate(agentId, ownerId);
        var reactivatedAgent = agent.reactivate();
        var savedAgent = agentRepository.save(reactivatedAgent);
        eventPublisher.publish(new AgentReactivated(savedAgent.agentId(), savedAgent.updatedAt()));
        log.info("Agent reactivated agentId={}", agentId);
        return savedAgent;
    }

    @Transactional
    public Agent updatePolicy(UUID agentId, UUID ownerId, String policyHash) {
        agentValidator.validatePolicyUpdate(policyHash);
        var agent = findAgentForUpdate(agentId, ownerId);
        var now = Instant.now();
        var updatedAgent = agent.toBuilder()
                .policyHash(policyHash)
                .updatedAt(now)
                .build();
        var savedAgent = agentRepository.save(updatedAgent);
        eventPublisher.publish(new AgentPolicyUpdated(savedAgent.agentId(), policyHash, now));
        log.info("Agent policy updated agentId={}", agentId);
        return savedAgent;
    }

    private Agent findAgentForUpdate(UUID agentId, UUID ownerId) {
        var agent = agentRepository.findByIdForUpdate(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
        if (!agent.ownerId().equals(ownerId)) {
            throw new ForbiddenException("agent", ownerId);
        }
        return agent;
    }
}
