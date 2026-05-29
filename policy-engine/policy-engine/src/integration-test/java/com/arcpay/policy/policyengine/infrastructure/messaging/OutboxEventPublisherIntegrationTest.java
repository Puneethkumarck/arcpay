package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.port.EventPublisher;
import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventPublisherIntegrationTest extends FullContextIntegrationTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeOutboxEventPublisherAsActiveEventPublisherBean() {
        // given
        var beanClass = eventPublisher.getClass();

        // when
        var resolvedName = org.springframework.util.ClassUtils.getUserClass(beanClass).getSimpleName();

        // then
        assertThat(resolvedName).isEqualTo("OutboxEventPublisher");
    }

    @Test
    void shouldWriteOutboxRecordWithinCallerTransaction() {
        // given
        var agentId = UUID.randomUUID();
        var event = new PolicyCreated(
                UUID.randomUUID(),
                agentId,
                UUID.randomUUID(),
                1,
                "0xabc",
                Instant.now());

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publish(event));

        // then
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM policyengine_outbox_record WHERE record_key = ?",
                Integer.class,
                agentId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldRejectPublishOutsideTransaction() {
        // given
        var event = new PolicyCreated(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "0xabc",
                Instant.now());

        // when / then
        assertThatThrownBy(() -> eventPublisher.publish(event))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
}
