package com.arcpay.identity.agentidentity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import com.arcpay.platform.infrastructure.messaging.OutboxHeaders;
import io.namastack.outbox.Outbox;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private Outbox outbox;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> contextCaptor;

    @Test
    void shouldResolveKeyFromAgentIdField() {
        // given
        var publisher = new TestableOutboxEventPublisher(outbox);
        var agentId = UUID.randomUUID();
        var event = new AgentEvent(agentId, UUID.randomUUID(), Instant.now());

        // when
        publisher.publish(event);

        // then
        assertScheduledWithKeyAndEventId(event, agentId.toString());
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
        assertScheduledWithKeyAndEventId(event, ownerId.toString());
    }

    private void assertScheduledWithKeyAndEventId(Object expectedEvent, String expectedKey) {
        then(outbox).should().schedule(eventCaptor.capture(), keyCaptor.capture(), contextCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
        assertThat(keyCaptor.getValue()).isEqualTo(expectedKey);
        assertThat(contextCaptor.getValue())
                .containsKey(OutboxHeaders.EVENT_ID_CONTEXT_KEY)
                .extractingByKey(OutboxHeaders.EVENT_ID_CONTEXT_KEY)
                .satisfies(id -> assertThat(id).asString().isNotBlank());
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

    public record AgentEvent(UUID agentId, UUID ownerId, Instant createdAt) {
        public static final String TOPIC = "agent.registered";
    }

    public record OwnerEvent(UUID ownerId, Instant createdAt) {
        public static final String TOPIC = "owner.registered";
    }

    public record NoKeyEvent(String data) {
        public static final String TOPIC = "no.key";
    }
}
