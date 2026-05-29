package com.arcpay.policy.policyengine.infrastructure.db.policy;

import com.arcpay.policy.policyengine.domain.model.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface PolicyJpaRepository extends JpaRepository<PolicyEntity, UUID> {

    Optional<PolicyEntity> findByAgentIdAndStatus(UUID agentId, PolicyStatus status);

    Page<PolicyEntity> findByAgentIdOrderByVersionDesc(UUID agentId, Pageable pageable);

    Optional<PolicyEntity> findByAgentIdAndPolicyId(UUID agentId, UUID policyId);

    @Query("SELECT MAX(p.version) FROM PolicyEntity p WHERE p.agentId = :agentId")
    Optional<Integer> findMaxVersionByAgentId(@Param("agentId") UUID agentId);
}
