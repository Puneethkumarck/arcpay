package com.arcpay.policy.policyengine.infrastructure.messaging;

import com.arcpay.policy.policyengine.domain.event.PolicyCreated;
import com.arcpay.policy.policyengine.domain.event.PolicyViolationDetected;
import io.namastack.outbox.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private static final UUID AGENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private Outbox outbox;

    @Test
    void shouldPublishPolicyCreatedWithAgentIdAsPartitionKey() {
        // given
        var publisher = new OutboxEventPublisher(outbox);
        var event = new PolicyCreated(
                UUID.randomUUID(),
                AGENT_ID,
                UUID.randomUUID(),
                1,
                "0xabc",
                Instant.parse("2026-05-29T10:00:00Z"));

        // when
        publisher.publish(event);

        // then
        then(outbox).should().schedule(event, AGENT_ID.toString());
    }

    @Test
    void shouldPublishPolicyViolationDetectedWithAgentIdAsPartitionKey() {
        // given
        var publisher = new OutboxEventPublisher(outbox);
        var event = new PolicyViolationDetected(
                UUID.randomUUID(),
                AGENT_ID,
                UUID.randomUUID(),
                "DAILY_LIMIT",
                "Daily spending limit exceeded",
                new BigDecimal("100.000000"),
                Instant.parse("2026-05-29T10:00:00Z"));

        // when
        publisher.publish(event);

        // then
        then(outbox).should().schedule(event, AGENT_ID.toString());
    }

    @Test
    void shouldPublishWithMandatoryTransactionPropagation() throws NoSuchMethodException {
        // given
        Method publish = AbstractMethodHolder.publishMethod();

        // when
        var transactional = publish.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        // then
        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(MANDATORY);
    }

    private static final class AbstractMethodHolder {

        private static Method publishMethod() throws NoSuchMethodException {
            return com.arcpay.platform.infrastructure.messaging.AbstractOutboxEventPublisher.class
                    .getMethod("publish", Object.class);
        }
    }
}
