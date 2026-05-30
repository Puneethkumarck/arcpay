package com.arcpay.policy.policyengine.infrastructure.db.spending;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM ReservationEntity r
            WHERE r.agentId = :agentId AND r.status = com.arcpay.policy.policyengine.domain.model.ReservationStatus.HELD
            """)
    BigDecimal sumActiveHeldAmount(@Param("agentId") UUID agentId);
}
