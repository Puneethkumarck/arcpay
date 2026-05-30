package com.arcpay.payment.paymentexecution.infrastructure.db;

import com.arcpay.payment.paymentexecution.domain.exception.IdempotencyConflictException;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OTHER_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OTHER_IDEMPOTENCY_KEY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePaymentWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRepositoryAdapterIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentJpaRepository jpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindById() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);

        // when
        paymentRepository.save(payment);
        var loaded = paymentRepository.findById(payment.paymentId()).orElseThrow();

        // then
        assertThat(loaded)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(payment);
    }

    @Test
    void shouldFindByAgentIdAndIdempotencyKey() {
        // given
        var payment = somePayment(PaymentStatus.SCREENING);
        paymentRepository.save(payment);

        // when
        var result = paymentRepository.findByAgentIdAndIdempotencyKey(SOME_AGENT_ID, SOME_IDEMPOTENCY_KEY);

        // then
        assertThat(result).isPresent();
        assertThat(result.get())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(payment);
    }

    @Test
    void shouldRoundTripMetadataJsonb() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        var entity = jpaRepository.findById(payment.paymentId()).orElseThrow();
        entity.setMetadata(Map.of("invoiceId", "INV-2026-001"));
        jpaRepository.saveAndFlush(entity);

        // when
        var reloaded = jpaRepository.findById(payment.paymentId()).orElseThrow();

        // then
        assertThat(reloaded.getMetadata())
                .usingRecursiveComparison()
                .isEqualTo(Map.of("invoiceId", "INV-2026-001"));
    }

    @Test
    void shouldReturnExistingPaymentWhenSameKeyAndSameFingerprint() {
        // given
        var first = somePayment(PaymentStatus.SCREENING);
        paymentRepository.save(first);
        var replay = somePayment(PaymentStatus.PENDING).toBuilder()
                .paymentId(UUID.randomUUID())
                .build();

        // when
        var result = paymentRepository.save(replay);

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(first);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldThrowWhenSameKeyButDifferentFingerprint() {
        // given
        var first = somePayment(PaymentStatus.PENDING);
        paymentRepository.save(first);
        var conflicting = somePayment(PaymentStatus.PENDING).toBuilder()
                .paymentId(UUID.randomUUID())
                .requestFingerprint("0xdifferentfingerprint")
                .build();

        // when / then
        assertThatThrownBy(() -> paymentRepository.save(conflicting))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining(SOME_IDEMPOTENCY_KEY);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldResolveIdempotentReplayWithinTransactionWithoutPoisoning() {
        // given
        var first = somePayment(PaymentStatus.SCREENING);
        paymentRepository.save(first);
        var replay = somePayment(PaymentStatus.PENDING).toBuilder()
                .paymentId(UUID.randomUUID())
                .build();
        var transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        var result = transactionTemplate.execute(status -> paymentRepository.save(replay));

        // then
        assertThat(result)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(first);
        assertThat(jpaRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldFindByOwnerIdPaginated() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        var other = somePaymentWith(UUID.randomUUID(), SOME_OTHER_AGENT_ID, SOME_OTHER_IDEMPOTENCY_KEY, PaymentStatus.SCREENING);
        paymentRepository.save(payment);
        paymentRepository.save(other);

        // when
        var page = paymentRepository.findByOwnerId(SOME_OWNER_ID, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent())
                .usingRecursiveFieldByFieldElementComparatorOnFields(
                        "paymentId", "agentId", "ownerId", "idempotencyKey", "status")
                .containsExactlyInAnyOrder(payment, other);
    }

    @Test
    void shouldFindByOwnerIdAndAgentIdPaginated() {
        // given
        var payment = somePayment(PaymentStatus.PENDING);
        var other = somePaymentWith(UUID.randomUUID(), SOME_OTHER_AGENT_ID, SOME_OTHER_IDEMPOTENCY_KEY, PaymentStatus.PENDING);
        paymentRepository.save(payment);
        paymentRepository.save(other);

        // when
        var page = paymentRepository.findByOwnerIdAndAgentId(SOME_OWNER_ID, SOME_AGENT_ID, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(payment));
    }

    @Test
    void shouldFindByOwnerIdAndAgentIdAndStatusPaginated() {
        // given
        var screening = somePayment(PaymentStatus.SCREENING);
        var completed = somePaymentWith(UUID.randomUUID(), SOME_AGENT_ID, SOME_OTHER_IDEMPOTENCY_KEY, PaymentStatus.COMPLETED);
        paymentRepository.save(screening);
        paymentRepository.save(completed);

        // when
        var page = paymentRepository.findByOwnerIdAndAgentIdAndStatus(
                SOME_OWNER_ID, SOME_AGENT_ID, PaymentStatus.SCREENING, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(screening));
    }

    @Test
    void shouldFindByOwnerIdAndStatusPaginated() {
        // given
        var pending = somePayment(PaymentStatus.PENDING);
        var completed = somePaymentWith(UUID.randomUUID(), SOME_OTHER_AGENT_ID, SOME_OTHER_IDEMPOTENCY_KEY, PaymentStatus.COMPLETED);
        paymentRepository.save(pending);
        paymentRepository.save(completed);

        // when
        var page = paymentRepository.findByOwnerIdAndStatus(
                SOME_OWNER_ID, PaymentStatus.COMPLETED, PageRequest.of(0, 10));

        // then
        assertThat(page.getContent())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(completed));
    }

    @Test
    void shouldReturnEmptyWhenPaymentNotFound() {
        // given
        var randomId = UUID.randomUUID();

        // when
        var result = paymentRepository.findById(randomId);

        // then
        assertThat(result).isEmpty();
    }
}
