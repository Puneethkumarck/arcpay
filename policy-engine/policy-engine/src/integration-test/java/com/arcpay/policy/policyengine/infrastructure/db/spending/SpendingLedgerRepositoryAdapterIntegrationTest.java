package com.arcpay.policy.policyengine.infrastructure.db.spending;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import com.arcpay.policy.policyengine.domain.port.SpendingLedgerRepository;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SpendingLedgerRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SpendingLedgerRepository spendingLedgerRepository;

    @Test
    void shouldSaveAndFindByPaymentId() {
        // given
        var entry = SpendingLedgerEntry.builder()
                .entryId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("100.000000"))
                .recipient("0x1234567890abcdef1234567890abcdef12345678")
                .executedAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .build();

        // when
        spendingLedgerRepository.save(entry);
        var loaded = spendingLedgerRepository.findByPaymentId(entry.paymentId()).orElseThrow();

        // then
        assertThat(loaded).usingRecursiveComparison().isEqualTo(entry);
    }

    @Test
    void shouldReturnSpendingSummary() {
        // given
        var agentId = UUID.randomUUID();
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        var executedAt = now.minus(1, ChronoUnit.HOURS);
        var recentEntry = SpendingLedgerEntry.builder()
                .entryId(UUID.randomUUID())
                .agentId(agentId)
                .paymentId(UUID.randomUUID())
                .amount(new BigDecimal("50.000000"))
                .recipient("0x1234567890abcdef1234567890abcdef12345678")
                .executedAt(executedAt)
                .createdAt(now)
                .build();
        spendingLedgerRepository.save(recentEntry);

        // when
        var summary = spendingLedgerRepository.getSpendingSummary(agentId, 120);

        // then
        var expected = SpendingSummary.builder()
                .dailyTotal(new BigDecimal("50.000000"))
                .weeklyTotal(new BigDecimal("50.000000"))
                .monthlyTotal(new BigDecimal("50.000000"))
                .velocityCount(1)
                .lastTransactionAt(executedAt)
                .build();
        assertThat(summary)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnZerosWhenNoSpending() {
        // given
        var agentId = UUID.randomUUID();

        // when
        var summary = spendingLedgerRepository.getSpendingSummary(agentId, 60);

        // then
        var expected = SpendingSummary.builder()
                .dailyTotal(BigDecimal.ZERO)
                .weeklyTotal(BigDecimal.ZERO)
                .monthlyTotal(BigDecimal.ZERO)
                .velocityCount(0)
                .lastTransactionAt(null)
                .build();
        assertThat(summary)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }
}
