package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.model.SpendingLedgerEntry;
import com.arcpay.policy.policyengine.domain.port.SpendingLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.platform.test.TestUtils.eqIgnoring;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_AMOUNT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_EXECUTED_AT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_LEDGER_ENTRY;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_PAYMENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_RECIPIENT;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_SPENDING_SUMMARY;
import static com.arcpay.policy.policyengine.test.fixtures.SpendingFixtures.SOME_VELOCITY_MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SpendingLedgerServiceTest {

    @Mock
    private SpendingLedgerRepository spendingLedgerRepository;

    @InjectMocks
    private SpendingLedgerService spendingLedgerService;

    @Test
    void shouldRecordSpending() {
        // given
        var expectedEntry = SpendingLedgerEntry.builder()
                .entryId(SOME_LEDGER_ENTRY.entryId())
                .agentId(SOME_AGENT_ID)
                .paymentId(SOME_PAYMENT_ID)
                .amount(SOME_AMOUNT)
                .recipient(SOME_RECIPIENT)
                .executedAt(SOME_EXECUTED_AT)
                .createdAt(SOME_LEDGER_ENTRY.createdAt())
                .build();
        given(spendingLedgerRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(spendingLedgerRepository.save(eqIgnoring(expectedEntry, "entryId", "createdAt")))
                .willReturn(expectedEntry);

        // when
        var result = spendingLedgerService.recordSpending(
                SOME_AGENT_ID, SOME_PAYMENT_ID, SOME_AMOUNT, SOME_RECIPIENT, SOME_EXECUTED_AT);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedEntry);
        then(spendingLedgerRepository).should().save(eqIgnoring(expectedEntry, "entryId", "createdAt"));
    }

    @Test
    void shouldGenerateUuidV7EntryId() {
        // given
        given(spendingLedgerRepository.findByPaymentId(SOME_PAYMENT_ID)).willReturn(Optional.empty());
        given(spendingLedgerRepository.save(eqIgnoring(SOME_LEDGER_ENTRY, "entryId", "createdAt")))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = spendingLedgerService.recordSpending(
                SOME_AGENT_ID, SOME_PAYMENT_ID, SOME_AMOUNT, SOME_RECIPIENT, SOME_EXECUTED_AT);

        // then
        assertThat(result.entryId().version()).isEqualTo(7);
    }

    @Test
    void shouldReturnExistingOnDuplicatePaymentId() {
        // given
        given(spendingLedgerRepository.findByPaymentId(SOME_PAYMENT_ID))
                .willReturn(Optional.of(SOME_LEDGER_ENTRY));

        // when
        var result = spendingLedgerService.recordSpending(
                SOME_AGENT_ID, SOME_PAYMENT_ID, SOME_AMOUNT, SOME_RECIPIENT, SOME_EXECUTED_AT);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_LEDGER_ENTRY);
        then(spendingLedgerRepository).should().findByPaymentId(SOME_PAYMENT_ID);
        then(spendingLedgerRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldReturnSpendingSummary() {
        // given
        given(spendingLedgerRepository.getSpendingSummary(SOME_AGENT_ID, SOME_VELOCITY_MINUTES))
                .willReturn(SOME_SPENDING_SUMMARY);

        // when
        var result = spendingLedgerService.getSpendingSummary(SOME_AGENT_ID, SOME_VELOCITY_MINUTES);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(SOME_SPENDING_SUMMARY);
        then(spendingLedgerRepository).should().getSpendingSummary(SOME_AGENT_ID, SOME_VELOCITY_MINUTES);
    }
}
