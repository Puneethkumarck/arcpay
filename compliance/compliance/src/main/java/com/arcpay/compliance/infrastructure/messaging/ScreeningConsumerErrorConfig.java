package com.arcpay.compliance.infrastructure.messaging;

import com.arcpay.compliance.domain.exception.MalformedAddressException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
class ScreeningConsumerErrorConfig {

    static final String DLT_SUFFIX = ".dlt";

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate, MeterRegistry meterRegistry) {
        var dltCounter = Counter.builder("compliance.screening.dlt")
                .description("Screening messages routed to the dead-letter topic after processing failure")
                .register(meterRegistry);

        var recoverer = new DeadLetterPublishingRecoverer(dltTemplates(kafkaTemplate),
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1));

        var backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5000L);
        backOff.setMaxAttempts(3);

        var errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(MalformedAddressException.class, DeserializationException.class);
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
                dltCounter.increment();
            }
        });
        return errorHandler;
    }

    private Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> dltTemplates(
            KafkaTemplate<String, Object> jsonTemplate) {
        var byteArrayProducerFactory = new DefaultKafkaProducerFactory<String, byte[]>(
                jsonTemplate.getProducerFactory().getConfigurationProperties(),
                new StringSerializer(), new ByteArraySerializer());

        var templates = new LinkedHashMap<Class<?>, KafkaOperations<? extends Object, ? extends Object>>();
        templates.put(byte[].class, new KafkaTemplate<>(byteArrayProducerFactory));
        templates.put(Object.class, jsonTemplate);
        return templates;
    }
}
