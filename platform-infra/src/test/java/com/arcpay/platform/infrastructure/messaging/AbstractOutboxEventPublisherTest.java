package com.arcpay.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import io.namastack.outbox.Outbox;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxEventPublisherTest {

    @Mock
    private Outbox outbox;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> contextCaptor;

    @Test
    void shouldScheduleWithResolvedKeyAndNonBlankEventIdContext() {
        // given
        var publisher = new TestOutboxEventPublisher(outbox);
        var agentId = UUID.randomUUID();
        var event = new TestEvent(agentId);

        // when
        publisher.publish(event);

        // then
        then(outbox).should().schedule(eventCaptor.capture(), keyCaptor.capture(), contextCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(event);
        assertThat(keyCaptor.getValue()).isEqualTo(agentId.toString());
        assertThat(contextCaptor.getValue())
                .containsKey(OutboxHeaders.EVENT_ID_CONTEXT_KEY)
                .extractingByKey(OutboxHeaders.EVENT_ID_CONTEXT_KEY)
                .satisfies(id -> assertThat(id).asString().isNotBlank());
    }

    @Test
    void shouldGenerateDistinctEventIdPerPublish() {
        // given
        var publisher = new TestOutboxEventPublisher(outbox);
        var event = new TestEvent(UUID.randomUUID());

        // when
        publisher.publish(event);
        publisher.publish(event);

        // then
        then(outbox).should(times(2)).schedule(eventCaptor.capture(), keyCaptor.capture(), contextCaptor.capture());
        var eventIds = contextCaptor.getAllValues().stream()
                .map(ctx -> ctx.get(OutboxHeaders.EVENT_ID_CONTEXT_KEY))
                .distinct()
                .toList();
        assertThat(eventIds).hasSize(2);
    }

    private static final class TestOutboxEventPublisher extends AbstractOutboxEventPublisher {
        TestOutboxEventPublisher(Outbox outbox) {
            super(outbox, List.of("agentId"));
        }
    }

    public record TestEvent(UUID agentId) {
        public static final String TOPIC = "test.event";
    }
}
