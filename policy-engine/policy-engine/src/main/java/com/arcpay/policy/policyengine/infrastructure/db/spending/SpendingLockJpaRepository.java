package com.arcpay.policy.policyengine.infrastructure.db.spending;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpendingLockJpaRepository extends JpaRepository<SpendingLockEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpendingLockEntity s WHERE s.agentId = :agentId")
    Optional<SpendingLockEntity> findByAgentIdForUpdate(@Param("agentId") UUID agentId);

    @Modifying
    @Query(value = """
            INSERT INTO spending_locks (agent_id, created_at)
            VALUES (:agentId, now())
            ON CONFLICT (agent_id) DO NOTHING
            """, nativeQuery = true)
    void insertIfNotExists(@Param("agentId") UUID agentId);
}
