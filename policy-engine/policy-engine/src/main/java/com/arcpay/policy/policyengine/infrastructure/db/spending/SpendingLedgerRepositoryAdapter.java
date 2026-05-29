package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import com.arcpay.policy.policyengine.domain.port.SpendingLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SpendingLedgerRepositoryAdapter implements SpendingLedgerRepository {

    private final SpendingLedgerJpaRepository jpaRepository;

    @Override
    public SpendingLedgerEntry save(SpendingLedgerEntry entry) {
        var entity = mapToEntity(entry);
        var saved = jpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<SpendingLedgerEntry> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(this::mapToDomain);
    }

    @Override
    public SpendingSummary getSpendingSummary(UUID agentId, int velocityMinutes) {
        var now = Instant.now();
        var dailyCutoff = now.minus(1, ChronoUnit.DAYS);
        var weeklyCutoff = now.minus(7, ChronoUnit.DAYS);
        var monthlyCutoff = now.minus(30, ChronoUnit.DAYS);
        var velocityCutoff = now.minus(velocityMinutes, ChronoUnit.MINUTES);

        var result = jpaRepository.getSpendingSummary(agentId, dailyCutoff, weeklyCutoff, monthlyCutoff, velocityCutoff);

        if (result == null || result.length == 0 || result[0] == null) {
            return SpendingSummary.builder()
                    .dailyTotal(BigDecimal.ZERO)
                    .weeklyTotal(BigDecimal.ZERO)
                    .monthlyTotal(BigDecimal.ZERO)
                    .velocityCount(0)
                    .lastTransactionAt(null)
                    .build();
        }

        var row = (Object[]) result[0];
        return SpendingSummary.builder()
                .dailyTotal(toBigDecimal(row[0]))
                .weeklyTotal(toBigDecimal(row[1]))
                .monthlyTotal(toBigDecimal(row[2]))
                .velocityCount(((Number) row[3]).intValue())
                .lastTransactionAt(toInstant(row[4]))
                .build();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }

    private static Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof Instant inst) return inst;
        return null;
    }

    private SpendingLedgerEntity mapToEntity(SpendingLedgerEntry entry) {
        return SpendingLedgerEntity.builder()
                .entryId(entry.entryId())
                .agentId(entry.agentId())
                .paymentId(entry.paymentId())
                .amount(entry.amount())
                .recipient(entry.recipient())
                .executedAt(entry.executedAt())
                .createdAt(entry.createdAt())
                .build();
    }

    private SpendingLedgerEntry mapToDomain(SpendingLedgerEntity entity) {
        return SpendingLedgerEntry.builder()
                .entryId(entity.getEntryId())
                .agentId(entity.getAgentId())
                .paymentId(entity.getPaymentId())
                .amount(entity.getAmount())
                .recipient(entity.getRecipient())
                .executedAt(entity.getExecutedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
