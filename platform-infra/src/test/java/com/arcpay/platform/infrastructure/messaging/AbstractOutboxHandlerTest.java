package com.arcpay.platform.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import io.namastack.outbox.handler.OutboxRecordMetadata;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxHandlerTest {

    private static final String KEY = "11111111-2222-3333-4444-555555555555";
    private static final String EVENT_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxRecordMetadata metadata;

    @Test
    void shouldPublishWithEventIdHeaderFromOutboxContext() {
        // given
        var handler = new TestOutboxHandler(kafkaTemplate);
        var event = new TestEvent("payload");
        var expected = new ProducerRecord<String, Object>(TestEvent.TOPIC, null, KEY, event);
        expected.headers()
                .add(new RecordHeader(OutboxHeaders.EVENT_ID_HEADER, EVENT_ID.getBytes(StandardCharsets.UTF_8)));
        given(metadata.getKey()).willReturn(KEY);
        given(metadata.getContext()).willReturn(Map.of(OutboxHeaders.EVENT_ID_CONTEXT_KEY, EVENT_ID));
        given(kafkaTemplate.send(expected)).willReturn(CompletableFuture.completedFuture(null));

        // when
        handler.handle(event, metadata);

        // then
        then(kafkaTemplate).should().send(expected);
    }

    @Test
    void shouldPublishWithoutHeaderWhenEventIdAbsentFromContext() {
        // given
        var handler = new TestOutboxHandler(kafkaTemplate);
        var event = new TestEvent("payload");
        var expected = new ProducerRecord<String, Object>(TestEvent.TOPIC, null, KEY, event);
        given(metadata.getKey()).willReturn(KEY);
        given(metadata.getContext()).willReturn(Map.of());
        given(kafkaTemplate.send(expected)).willReturn(CompletableFuture.completedFuture(null));

        // when
        handler.handle(event, metadata);

        // then
        then(kafkaTemplate).should().send(expected);
        assertThat(expected.headers().lastHeader(OutboxHeaders.EVENT_ID_HEADER)).isNull();
    }

    private static final class TestOutboxHandler extends AbstractOutboxHandler {
        TestOutboxHandler(KafkaTemplate<String, Object> kafkaTemplate) {
            super(kafkaTemplate);
        }
    }

    public record TestEvent(String data) {
        public static final String TOPIC = "test.event";
    }
}
