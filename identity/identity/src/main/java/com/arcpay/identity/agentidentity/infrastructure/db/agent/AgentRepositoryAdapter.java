package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.infrastructure.db.agent.mapper.AgentEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AgentRepositoryAdapter implements AgentRepository {

    private final AgentJpaRepository jpaRepository;
    private final AgentEntityMapper mapper;

    @Override
    public Agent save(Agent agent) {
        var entity = mapper.mapToEntity(agent);
        var saved = jpaRepository.save(entity);
        return mapper.mapToDomain(saved);
    }

    @Override
    public Optional<Agent> findById(UUID agentId) {
        return jpaRepository.findById(agentId).map(mapper::mapToDomain);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Agent> findByIdForUpdate(UUID agentId) {
        return jpaRepository.findByIdForUpdate(agentId).map(mapper::mapToDomain);
    }

    @Override
    public Page<Agent> findByOwnerIdAndStatus(UUID ownerId, AgentStatus status, Pageable pageable) {
        return jpaRepository.findByOwnerIdAndStatus(ownerId, status, pageable)
                .map(mapper::mapToDomain);
    }

    @Override
    public Page<Agent> findByOwnerId(UUID ownerId, Pageable pageable) {
        return jpaRepository.findByOwnerId(ownerId, pageable)
                .map(mapper::mapToDomain);
    }

    @Override
    public boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name) {
        return jpaRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, name);
    }

    @Override
    public boolean existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(UUID ownerId, String name, UUID agentId) {
        return jpaRepository.existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(ownerId, name, agentId);
    }
}
