package com.arcpay.platform.infrastructure.messaging;

import io.namastack.outbox.handler.OutboxRecordMetadata;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOutboxHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @io.namastack.outbox.annotation.OutboxHandler
    public void handle(Object event, OutboxRecordMetadata metadata) {
        var topic = resolveStaticField(event, "TOPIC");
        var key = metadata.getKey();
        var eventId = metadata.getContext().get(OutboxHeaders.EVENT_ID_CONTEXT_KEY);
        try {
            kafkaTemplate.send(buildRecord(topic, key, event, eventId)).get(10, TimeUnit.SECONDS);
            log.debug(
                    "Published outbox event type={} topic={} key={} eventId={}",
                    event.getClass().getSimpleName(),
                    topic,
                    key,
                    eventId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Kafka send interrupted for event " + event.getClass().getSimpleName(), e);
        } catch (Exception e) {
            log.error(
                    "Failed to publish event type={} topic={}: {}",
                    event.getClass().getSimpleName(),
                    topic,
                    e.getMessage());
            throw new RuntimeException(
                    "Kafka send failed for event " + event.getClass().getSimpleName(), e);
        }
    }

    private ProducerRecord<String, Object> buildRecord(String topic, String key, Object event, String eventId) {
        var record = new ProducerRecord<String, Object>(topic, null, key, event);
        if (eventId != null) {
            record.headers()
                    .add(new RecordHeader(OutboxHeaders.EVENT_ID_HEADER, eventId.getBytes(StandardCharsets.UTF_8)));
        }
        return record;
    }

    private String resolveStaticField(Object event, String fieldName) {
        try {
            var value = event.getClass().getField(fieldName).get(null);
            if (!(value instanceof String topic)) {
                throw new IllegalArgumentException("Event class static " + fieldName + " must be String: "
                        + event.getClass().getName());
            }
            return topic;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "Event class missing static " + fieldName + " field: "
                            + event.getClass().getName(),
                    e);
        }
    }
}
