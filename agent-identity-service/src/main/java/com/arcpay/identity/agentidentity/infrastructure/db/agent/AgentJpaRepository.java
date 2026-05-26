package com.arcpay.identity.agentidentity.infrastructure.db.agent;

import com.arcpay.identity.agentidentity.domain.model.AgentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentEntity a WHERE a.agentId = :agentId")
    Optional<AgentEntity> findByIdForUpdate(@Param("agentId") UUID agentId);

    Page<AgentEntity> findByOwnerIdAndStatus(UUID ownerId, AgentStatus status, Pageable pageable);

    Page<AgentEntity> findByOwnerId(UUID ownerId, Pageable pageable);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCaseAndAgentIdNot(UUID ownerId, String name, UUID agentId);
}
