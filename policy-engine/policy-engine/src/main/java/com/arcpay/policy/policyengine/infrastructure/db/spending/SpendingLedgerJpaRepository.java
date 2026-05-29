package com.arcpay.policy.policyengine.infrastructure.db.spending;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface SpendingLedgerJpaRepository extends JpaRepository<SpendingLedgerEntity, UUID> {

    Optional<SpendingLedgerEntity> findByPaymentId(UUID paymentId);

    @Query(value = """
            SELECT COALESCE(SUM(s.amount) FILTER (WHERE s.executed_at > :dailyCutoff), 0) as dailyTotal,
                   COALESCE(SUM(s.amount) FILTER (WHERE s.executed_at > :weeklyCutoff), 0) as weeklyTotal,
                   COALESCE(SUM(s.amount) FILTER (WHERE s.executed_at > :monthlyCutoff), 0) as monthlyTotal,
                   COALESCE(COUNT(*) FILTER (WHERE s.executed_at > :velocityCutoff), 0) as velocityCount,
                   MAX(s.executed_at) as lastTransactionAt
            FROM spending_ledger s WHERE s.agent_id = :agentId
            """, nativeQuery = true)
    Object[] getSpendingSummary(@Param("agentId") UUID agentId,
                                @Param("dailyCutoff") Instant dailyCutoff,
                                @Param("weeklyCutoff") Instant weeklyCutoff,
                                @Param("monthlyCutoff") Instant monthlyCutoff,
                                @Param("velocityCutoff") Instant velocityCutoff);
}
