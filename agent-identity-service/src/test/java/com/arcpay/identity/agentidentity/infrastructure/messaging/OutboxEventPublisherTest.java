package com.arcpay.identity.agentidentity.infrastructure.messaging;

import io.namastack.outbox.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private Outbox outbox;

    @Test
    void shouldResolveKeyFromAgentIdField() {
        // given
        var publisher = new TestableOutboxEventPublisher(outbox);
        var agentId = UUID.randomUUID();
        var event = new AgentEvent(agentId, UUID.randomUUID(), Instant.now());

        // when
        publisher.publish(event);

        // then
        then(outbox).should().schedule(event, agentId.toString());
    }

    @Test
    void shouldResolveKeyFromOwnerIdWhenAgentIdMissing() {
        // given
        var publisher = new TestableOutboxEventPublisher(outbox);
        var ownerId = UUID.randomUUID();
        var event = new OwnerEvent(ownerId, Instant.now());

        // when
        publisher.publish(event);

        // then
        then(outbox).should().schedule(event, ownerId.toString());
    }

    @Test
    void shouldThrowWhenNoKeyFieldResolvable() {
        // given
        var publisher = new TestableOutboxEventPublisher(outbox);
        var event = new NoKeyEvent("data");

        // when / then
        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId")
                .hasMessageContaining("ownerId");
    }

    private static class TestableOutboxEventPublisher extends OutboxEventPublisher {
        TestableOutboxEventPublisher(Outbox outbox) {
            super(outbox);
        }
    }

    record AgentEvent(UUID agentId, UUID ownerId, Instant createdAt) {
        public static final String TOPIC = "agent.registered";
    }

    record OwnerEvent(UUID ownerId, Instant createdAt) {
        public static final String TOPIC = "owner.registered";
    }

    record NoKeyEvent(String data) {
        public static final String TOPIC = "no.key";
    }
}
