package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.model.SpendingSummary;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SpendingLedgerIntegrationTest extends FullContextIntegrationTest {

    private static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";

    @Autowired
    private SpendingLedgerService spendingLedgerService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRecordAndQuerySpendingSummary() {
        // given
        var agentId = UUID.randomUUID();
        var executedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("50.000000"), SOME_RECIPIENT, executedAt);

        // when
        var summary = spendingLedgerService.getSpendingSummary(agentId, 120);

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
    void shouldReturnZerosWhenNoSpendingExists() {
        // given
        var agentId = UUID.randomUUID();

        // when
        var summary = spendingLedgerService.getSpendingSummary(agentId, 60);

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

    @Test
    void shouldReturnCorrectRollingWindowTotals() {
        // given
        var agentId = UUID.randomUUID();
        var now = Instant.now();
        var within24Hours = now.minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var within7Days = now.minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        var within30Days = now.minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        var olderThan30Days = now.minus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);

        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("10.000000"), SOME_RECIPIENT, within24Hours);
        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("20.000000"), SOME_RECIPIENT, within7Days);
        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("30.000000"), SOME_RECIPIENT, within30Days);
        spendingLedgerService.recordSpending(agentId, UUID.randomUUID(),
                new BigDecimal("40.000000"), SOME_RECIPIENT, olderThan30Days);

        // when
        var summary = spendingLedgerService.getSpendingSummary(agentId, 60);

        // then
        var expected = SpendingSummary.builder()
                .dailyTotal(new BigDecimal("10.000000"))
                .weeklyTotal(new BigDecimal("30.000000"))
                .monthlyTotal(new BigDecimal("60.000000"))
                .velocityCount(0)
                .lastTransactionAt(within24Hours)
                .build();
        assertThat(summary)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    void shouldHandleDuplicatePaymentIdGracefully() {
        // given
        var agentId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var executedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var first = spendingLedgerService.recordSpending(agentId, paymentId,
                new BigDecimal("50.000000"), SOME_RECIPIENT, executedAt);

        // when
        var second = spendingLedgerService.recordSpending(agentId, paymentId,
                new BigDecimal("999.000000"), SOME_RECIPIENT, executedAt);

        // then
        assertThat(second).usingRecursiveComparison().isEqualTo(first);
        var summary = spendingLedgerService.getSpendingSummary(agentId, 120);
        var expectedSummary = SpendingSummary.builder()
                .dailyTotal(new BigDecimal("50.000000"))
                .weeklyTotal(new BigDecimal("50.000000"))
                .monthlyTotal(new BigDecimal("50.000000"))
                .velocityCount(1)
                .lastTransactionAt(executedAt)
                .build();
        assertThat(summary)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedSummary);
    }

    @Test
    void shouldNoOpDuplicatePaymentIdAcrossSeparateTransactions() {
        // given
        var agentId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var executedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        var transactionTemplate = new TransactionTemplate(transactionManager);
        // REQUIRES_NEW so each call commits independently of the surrounding
        // class-level @Transactional test transaction, exercising the real cross-tx path
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // when — each call commits in its own transaction, so the second call's
        // findByPaymentId observes the first row committed by the UNIQUE-constrained
        // payment_id and must idempotently no-op to that single row
        SpendingLedgerEntry first = transactionTemplate.execute(status ->
                spendingLedgerService.recordSpending(agentId, paymentId,
                        new BigDecimal("50.000000"), SOME_RECIPIENT, executedAt));
        SpendingLedgerEntry second = transactionTemplate.execute(status ->
                spendingLedgerService.recordSpending(agentId, paymentId,
                        new BigDecimal("999.000000"), SOME_RECIPIENT, executedAt));

        try {
            // then — second returns the same persisted entry, never the second amount
            assertThat(second).usingRecursiveComparison().isEqualTo(first);

            var summary = transactionTemplate.execute(status ->
                    spendingLedgerService.getSpendingSummary(agentId, 120));
            var expectedSummary = SpendingSummary.builder()
                    .dailyTotal(new BigDecimal("50.000000"))
                    .weeklyTotal(new BigDecimal("50.000000"))
                    .monthlyTotal(new BigDecimal("50.000000"))
                    .velocityCount(1)
                    .lastTransactionAt(executedAt)
                    .build();
            assertThat(summary)
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expectedSummary);
        } finally {
            // committed rows escape the class-level @Transactional rollback; clean up
            // in its own committed transaction too
            transactionTemplate.executeWithoutResult(status ->
                    jdbcTemplate.update("DELETE FROM spending_ledger WHERE payment_id = ?", paymentId));
        }
    }
}
