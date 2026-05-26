package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.Agent;
import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {

    Agent save(Agent agent);

    Optional<Agent> findById(UUID agentId);

    Optional<Agent> findByIdForUpdate(UUID agentId);

    Page<Agent> findByOwnerIdAndStatus(UUID ownerId, AgentStatus status, Pageable pageable);

    Page<Agent> findByOwnerId(UUID ownerId, Pageable pageable);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(UUID ownerId, String name, UUID agentId);
}
