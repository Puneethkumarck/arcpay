package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOutboxEventPublisherIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM paymentexecution_outbox_record");
    }

    @Test
    void shouldScheduleEventWithinTransaction() {
        // given
        var event = PaymentRequested.builder()
                .paymentId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .walletId("wallet-1")
                .idempotencyKey("idem-1")
                .recipientAddress("0xabc")
                .amount(new BigDecimal("25.00"))
                .currency("USDC")
                .memo("memo")
                .metadata(Map.of("k", "v"))
                .requestedAt(Instant.parse("2026-05-29T10:00:00Z"))
                .build();
        var transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        // then
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM paymentexecution_outbox_record WHERE record_key = ?",
                Long.class, event.paymentId().toString());
        assertThat(count).isEqualTo(1);
    }
}
