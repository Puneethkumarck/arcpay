package com.arcpay.settlement.infrastructure.db;

import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.arcpay.settlement.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someSettlementTransaction;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someTransactionWith;
import static org.assertj.core.api.Assertions.assertThat;

class SettlementTransactionRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private SettlementTransactionRepository repository;

    @Autowired
    private SettlementTransactionJpaRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByPaymentId() {
        // given
        var transaction = someSettlementTransaction(TransferState.SENT);

        // when
        repository.save(transaction);
        var loaded = repository.findByPaymentId(SOME_PAYMENT_ID).orElseThrow();

        // then
        assertThat(loaded)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(transaction);
    }

    @Test
    void shouldPersistStateAsString() {
        // given
        var transaction = someSettlementTransaction(TransferState.CONFIRMED);
        repository.save(transaction);

        // when
        var persistedState = jpaRepository.findById(SOME_PAYMENT_ID).orElseThrow().getState();

        // then
        assertThat(persistedState).isEqualTo(TransferState.CONFIRMED);
    }

    @Test
    void shouldReturnExistingTransactionWhenSamePaymentIdResaved() {
        // given
        var first = someSettlementTransaction(TransferState.SENT);
        repository.save(first);
        var retry = someSettlementTransaction(TransferState.CONFIRMED);

        // when
        var result = repository.save(retry);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(first);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldResolveIdempotentResaveWithinTransactionWithoutPoisoning() {
        // given
        var first = someSettlementTransaction(TransferState.SENT);
        repository.save(first);
        var retry = someSettlementTransaction(TransferState.CONFIRMED);
        var transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        var result = transactionTemplate.execute(status -> repository.save(retry));

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(first);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldFindByCircleTxId() {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));

        // when
        var loaded = repository.findByCircleTxId(SOME_CIRCLE_TX_ID);

        // then
        assertThat(loaded).get()
                .extracting(tx -> tx.paymentId())
                .isEqualTo(SOME_PAYMENT_ID);
    }

    @Test
    void shouldReturnEmptyWhenCircleTxIdNotFound() {
        // when
        var loaded = repository.findByCircleTxId("missing-circle-tx");

        // then
        assertThat(loaded).isEmpty();
    }

    @Test
    void shouldUpdateExistingTransactionState() {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var transition = someSettlementTransaction(TransferState.COMPLETED);

        // when
        repository.update(transition);

        // then
        assertThat(repository.findByPaymentId(SOME_PAYMENT_ID).orElseThrow().state())
                .isEqualTo(TransferState.COMPLETED);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyWhenPaymentIdNotFound() {
        // given
        var randomId = UUID.randomUUID();

        // when
        var result = repository.findByPaymentId(randomId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSaveDistinctTransactions() {
        // given
        var first = someSettlementTransaction(TransferState.SENT);
        var second = someTransactionWith(UUID.randomUUID(), TransferState.COMPLETED);

        // when
        repository.save(first);
        repository.save(second);

        // then
        assertThat(jpaRepository.count()).isEqualTo(2);
    }
}
