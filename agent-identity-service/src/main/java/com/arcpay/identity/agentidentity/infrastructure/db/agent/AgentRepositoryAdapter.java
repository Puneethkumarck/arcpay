package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.infrastructure.db.agent.mapper.AgentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AgentRepositoryAdapter implements AgentRepository {

    private final AgentJpaRepository agentJpaRepository;
    private final AgentEntityMapper agentEntityMapper;

    @Override
    public Agent save(Agent agent) {
        var entity = agentEntityMapper.mapToEntity(agent);
        var saved = agentJpaRepository.save(entity);
        return agentEntityMapper.mapToDomain(saved);
    }

    @Override
    public Optional<Agent> findById(UUID agentId) {
        return agentJpaRepository.findById(agentId).map(agentEntityMapper::mapToDomain);
    }

    @Override
    public Optional<Agent> findByIdForUpdate(UUID agentId) {
        return agentJpaRepository.findByIdForUpdate(agentId).map(agentEntityMapper::mapToDomain);
    }

    @Override
    public Page<Agent> findByOwnerIdAndStatus(UUID ownerId, AgentStatus status, Pageable pageable) {
        return agentJpaRepository.findByOwnerIdAndStatus(ownerId, status, pageable)
                .map(agentEntityMapper::mapToDomain);
    }

    @Override
    public Page<Agent> findByOwnerId(UUID ownerId, Pageable pageable) {
        return agentJpaRepository.findByOwnerId(ownerId, pageable)
                .map(agentEntityMapper::mapToDomain);
    }

    @Override
    public boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name) {
        return agentJpaRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name);
    }

    @Override
    public boolean existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(UUID ownerId, String name, UUID agentId) {
        return agentJpaRepository.existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(ownerId, name, agentId);
    }
}
